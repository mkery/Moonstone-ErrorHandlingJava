package edu.cmu.moonstone.ghostcomment;

import moonstone.EditorUtility;
import moonstone.ParentDelegate;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.*;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class GhostCommentPainter implements IPainter, PaintListener, CaretListener, IElementChangedListener, MouseTrackListener, GhostCommentControl.EditorDelegate {

	private final ITextEditor editor;
	private final ITextViewer viewer;

	private GC gc;

	private TryStatement currentTry = null;
	private List<CommentInfo> ghostComments = new ArrayList<>(0);

	private boolean dialogOpen;

	public GhostCommentPainter(IEditorPart editorPart) {
		editor = editorPart.getAdapter(ITextEditor.class);

		ITextOperationTarget target = editorPart.getAdapter(ITextOperationTarget.class);
		viewer = (ITextViewer)target;
	}

	public void addToEditor() {
		if (!(editor.getEditorInput() instanceof IFileEditorInput)) return;

		ITextViewerExtension2 extension = (ITextViewerExtension2)viewer;
		extension.addPainter(this);
	}

	@Override
	public void paintControl(PaintEvent ev) {
		if (currentTry == null) return;

		highlightTry(ev.gc);
        paintGhostComments(ev.gc);
    }

    private void paintGhostComments(GC gc) {
		ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
		StyledText st = viewer.getTextWidget();

		Color oldForeground = gc.getForeground();
		try {
			for (CommentInfo c : ghostComments) {
				// Skip actual comments, since they don't need to be painted
				if (!c.hasGhost()) continue;

				int commentOffset = extension5.modelOffset2WidgetOffset(c.getCommentOffset());
				Point p;
				try {
					p = st.getLocationAtOffset(commentOffset);
				} catch (IllegalArgumentException e) {
					continue;
				}
				gc.setForeground(new Color(gc.getDevice(), 128, 128, 128));
				gc.drawText(c.getText(), p.x, p.y, true);
			}
		}
		finally {
			gc.setForeground(oldForeground);
		}
    }

	private void highlightTry(GC gc) {
		Rectangle highlightTryRect = getHighlightTryRectangle(viewer, currentTry);
		if (highlightTryRect == null) return;

		Color oldBackground = gc.getBackground();
        int alpha = gc.getAlpha();
        try {
        	gc.setBackground(new Color(gc.getDevice(), 0, 0, 0));
        	gc.setAlpha(10);
			gc.fillRectangle(highlightTryRect);
        }
        finally {
			gc.setBackground(oldBackground);
			gc.setAlpha(alpha);
		}
    }

	private static Rectangle getHighlightTryRectangle(ITextViewer viewer, TryStatement currentTry) {
		ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
		StyledText st = viewer.getTextWidget();

		int leftMargin = st.getLeftMargin();
		int rightMargin = st.getRightMargin();
		Rectangle bounds = st.getClientArea();

		int startPosition = currentTry.getStartPosition();
		int widgetStart = extension5.modelOffset2WidgetOffset(startPosition);
		int widgetEnd = extension5.modelOffset2WidgetOffset(startPosition + currentTry.getLength() - 1);

		Rectangle tryBounds;
		try {
			tryBounds = st.getTextBounds(widgetStart, widgetEnd);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return new Rectangle(bounds.x + leftMargin, tryBounds.y, bounds.width - leftMargin - rightMargin, tryBounds.height);
	}

	private static final WeakHashMap<ITextEditor, GhostCommentPainter> activeGhostCommentPainter = new WeakHashMap<>();

	public static GhostCommentPainter getActive(ITextEditor editor) {
		return activeGhostCommentPainter.get(editor);
	}

	@Override
	public void paint(int reason) {
		if (gc == null) {
			StyledText st = viewer.getTextWidget();
			gc = new GC(st);
			activeGhostCommentPainter.put(editor, this);

			st.addPaintListener(this);
			st.addMouseTrackListener(this);
			st.addCaretListener(this);
			JavaCore.addElementChangedListener(this);
			//IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			//doc.addDocumentListener(this);
			codeSelectionChanged(st.getCaretOffset());
		}
	}

	@Override
	public void deactivate(boolean redraw) {
		if (gc != null) {
			if (selectionJob != null && !selectionJob.cancel()) {
				try {
					selectionJob.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			activeGhostCommentPainter.remove(editor, this);
			gc.dispose();
			gc = null;

			StyledText st = viewer.getTextWidget();
			st.removePaintListener(this);
			st.removeMouseTrackListener(this);
			st.removeCaretListener(this);
			JavaCore.removeElementChangedListener(this);
			currentTry = null;
			ghostComments = Collections.emptyList();

			if (redraw) st.redraw();
		}
	}

	@Override
	public void dispose() {
		deactivate(false);
	}

	@Override
	public void setPositionManager(IPaintPositionManager manager) {
	}

	@Override
	public void caretMoved(CaretEvent ev) {
		codeSelectionChanged(ev.caretOffset);
	}

	@Override
	public void elementChanged(ElementChangedEvent elementChangedEvent) {
		StyledText st = viewer.getTextWidget();
		st.getDisplay().asyncExec(() -> {
			if (st.isDisposed()) return;
			int caretOffset = st.getCaretOffset();
			codeSelectionChanged(caretOffset);
		});
	}

	private Job selectionJob;
	private void codeSelectionChanged(int widgetOffset) { // always executed on main thread
		if (selectionJob != null) selectionJob.cancel();
		selectionJob = Job.create("Selecting try block", monitor -> {
			ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
			int modelOffset = extension5.widgetOffset2ModelOffset(widgetOffset);
			TryStatement newParent = EditorUtility.inTryStatement(editor, modelOffset);

			currentTry = newParent;

			if (newParent != null) {
				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            	/* Build a list of all potential comments for this try block */
				ghostComments = EditorUtility.getCommentInfoThrownByTryBody(doc, gc, newParent);
			} else {
				ghostComments = Collections.emptyList();
			}

			StyledText textWidget = viewer.getTextWidget();
			if (textWidget != null) textWidget.getDisplay().asyncExec(textWidget::redraw);
		});
		selectionJob.setPriority(Job.INTERACTIVE);
		selectionJob.setRule(((IFileEditorInput) editor.getEditorInput()).getFile());
		selectionJob.schedule();
	}

	@Override
	public void mouseEnter(MouseEvent e) {
	}

	@Override
	public void mouseExit(MouseEvent e) {
	}

	@Override
	public void mouseHover(MouseEvent ev) {
		if(!dialogOpen)
		{
			Pair<CommentInfo, ExceptionInfo> hit = confirmHover(ev.x, ev.y);
			if (hit == null) return;

			StyledText st = viewer.getTextWidget();
			GhostCommentInputCreator inputCreator = new GhostCommentInputCreator(getEditor(), Arrays.asList(hit));
			Stream<Function<ParentDelegate, GhostCommentControl>> controlCreators = inputCreator.createControls(this);

			GhostCommentDialog gcDialog = new GhostCommentDialog(st.getShell(), st.toDisplay(ev.x, ev.y)
					, controlCreators, () -> dialogOpen = false);

			// Open the gcDialog
			dialogOpen = true;
			gcDialog.open();
			gcDialog.initCloseListener(st);
		}
	}

	@Override
	public boolean allowAcceptComment() {
		return true;
	}

	@Override
	public void acceptedComment(CommentInfo commentInfo) {
		codeSelectionChanged(commentInfo.getCommentOffset());
	}

	@Override
	public void jump(IRegion lineInformation) {
		//GoTo the lineNumber
		editor.selectAndReveal(lineInformation.getOffset(), lineInformation.getLength());
	}

	@Override
	public IEditorPart getEditor() {
		return editor;
	}

	public Pair<CommentInfo,ExceptionInfo> confirmHover(int x, int y) {
		for (CommentInfo commentInfo : ghostComments) {
			if (!commentInfo.hasGhost()) continue;
			ExceptionInfo exceptionInfo = commentInfo.confirmHover(viewer, x, y);
			if (exceptionInfo != null) return Pair.of(commentInfo, exceptionInfo);
        }
        return null;
	}

	public Pair<CommentInfo,ExceptionInfo> confirmHit(int offset) {
		for (CommentInfo commentInfo : ghostComments) {
			if (commentInfo.hasGhost()) continue;
			ExceptionInfo exceptionInfo = commentInfo.confirmHit(offset);
			if (exceptionInfo != null) return Pair.of(commentInfo, exceptionInfo);
		}
		return null;
	}

	public TryStatement getTryStatement() {
		return currentTry;
	}

	public List<CommentInfo> getGhostComments() {
		return ghostComments;
	}
}
