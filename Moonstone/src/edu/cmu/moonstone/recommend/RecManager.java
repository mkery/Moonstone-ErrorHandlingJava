package edu.cmu.moonstone.recommend;

import edu.cmu.moonstone.view.CatchExample;
import edu.cmu.moonstone.view.CustomView;
import moonstone.ASTStatementVisitor;
import moonstone.EditorUtility;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.ITextEditor;
import recommend.CatchHandler;
import recommend.RecDatabase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

public class RecManager
{
	private final ITextEditor editor;
	private final ITextViewer viewer;
	private Job selectionJob;

	public RecManager(IEditorPart editorPart)
	{
		editor = editorPart.getAdapter(ITextEditor.class);
		viewer = (ITextViewer) editorPart.getAdapter(ITextOperationTarget.class);
	}

	private void codeSelectionChanged(@SuppressWarnings("unused") TypedEvent e) {
		ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
		StyledText st = viewer.getTextWidget();

		Point selection = st.getSelection();
		if (selection.x == selection.y) {
			int caretOffset = extension5.widgetOffset2ModelOffset(st.getCaretOffset());
			showCustomView(caretOffset, 1, false, false);
		} else {
			int modelOffset = extension5.widgetOffset2ModelOffset(selection.x);
			int modelLength = extension5.widgetOffset2ModelOffset(selection.y) - modelOffset;
			showCustomView(modelOffset, modelLength, false, false);
		}
	}

	private int previousOffset;
	private int previousLength;
	private ASTNode previousNode;
	public void showCustomView(int modelOffset, int modelLength, boolean focusLog, boolean focusNonLog) {
		if (modelOffset == previousOffset && modelLength == previousLength) return;
		if (!(editor.getEditorInput() instanceof IFileEditorInput)) return;
		previousOffset = modelOffset;
		previousLength = modelLength;

		//This setups up the view
		if (selectionJob != null) selectionJob.cancel();
		selectionJob = Job.create("Selecting try recommendations", monitor -> {
			try {
				IFileEditorInput fileEditorInput = (IFileEditorInput) editor.getEditorInput();
				IFile file = fileEditorInput.getFile();
				IProject project = file.getProject();

				confirmSelect(modelOffset, modelLength, new ASTStatementVisitor() {
					@Override
					public boolean visit(CatchClause catchClause) {
						if (catchClause == previousNode) return false;
						previousNode = catchClause;

						List<String> exceptionTypes = CatchHandler.getExceptionTypes(catchClause);
						String exceptionType = String.join(" | ", exceptionTypes);
						Position pos = new Position(catchClause.getParent().getStartPosition(), catchClause.getParent().getLength());
						List<CatchExample[]> data = RecDatabase.retrieveMatches(exceptionTypes, project, file, pos);

						showData(exceptionType, data);
						return false;
					}

					@Override
					public boolean visit(Block block) {
						if (!TryStatement.FINALLY_PROPERTY.equals(block.getLocationInParent())) return true;
						if (block == previousNode) return false;
						previousNode = block;

						Position pos = new Position(block.getStartPosition(), block.getLength());
						List<CatchExample[]> data = RecDatabase.retrieveMatches(project, file, pos);
						showData("finally", data);
						return false;
					}

					@Override
					public boolean visit(TryStatement tryStatement) {
						if (tryStatement == previousNode) return false;
						previousNode = tryStatement;

						List<CatchClause> catchClauses = tryStatement.catchClauses();
						List<String> exceptionTypes = catchClauses.stream()
								.map(CatchHandler::getExceptionTypes)
								.flatMap(Collection::stream)
								.collect(Collectors.toList());

						Position pos = new Position(tryStatement.getStartPosition(), tryStatement.getLength());
						List<CatchExample[]> data = RecDatabase.retrieveMatches(exceptionTypes, project, file, pos);

						if (tryStatement.getFinally() != null) {
							exceptionTypes.add(0, "finally");
							data.addAll(0, RecDatabase.retrieveMatches(project, file, pos));
						}

						String exceptionType = String.join(" | ", exceptionTypes);
						showData(exceptionType, data);
						return false;
					}

					private void showData(String exceptionType, List<CatchExample[]> data) {
						int fileCount = (int) data.stream()
								.flatMap(catchExamples -> Arrays.stream(catchExamples)
										.map(CatchExample::getClassPath))
								.distinct()
								.count();
						int exampleCount = data.size();

						//This setups up the view
						PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
							CustomView cv;
							try {
								cv = (CustomView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
										.showView(CustomView.ID, null,
												focusLog || focusNonLog ? IWorkbenchPage.VIEW_ACTIVATE : IWorkbenchPage.VIEW_VISIBLE);
							} catch (PartInitException e) {
								throw new RuntimeException(e);
							}

							cv.setupTitleDescription(exampleCount, exceptionType, fileCount);
							cv.setup(data);

							if (focusLog) cv.focusLog();
							else if (focusNonLog) cv.focusNonLog();
						});
					}
				});
			} catch (Throwable t) {
				System.err.println("Selecting try recommendations failed");
				t.printStackTrace();
			}
		});
		selectionJob.setPriority(Job.DECORATE);
		selectionJob.setRule(((IFileEditorInput) editor.getEditorInput()).getFile());
		selectionJob.schedule();
	}

	private static final WeakHashMap<ITextEditor, RecManager> activeRecManager = new WeakHashMap<>();

	public static RecManager getActive(ITextEditor editor) {
		return activeRecManager.get(editor);
	}

	/** Adds a listener to the current editorStyledText to check for mouse hovers
	 * 
	 */
	public void addStyledTextListener()
	{
		activeRecManager.put(editor, this);
		StyledText st = viewer.getTextWidget();
		st.addCaretListener(this::codeSelectionChanged);
		st.addExtendedModifyListener(this::codeSelectionChanged);
		//codeSelectionChanged(st.getCaretOffset()); //TODO: Retrieve recommendations on demand
	}

	private void confirmSelect(int modelOffset, int modelLength, ASTVisitor visitor) {
		ICompilationUnit cu = (ICompilationUnit) JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		ASTNode selectedNode = EditorUtility.getSelectedNode(cu, modelOffset, modelLength);
		EditorUtility.acceptParents(selectedNode, visitor);
	}
}
