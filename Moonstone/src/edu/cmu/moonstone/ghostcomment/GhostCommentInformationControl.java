package edu.cmu.moonstone.ghostcomment;

import moonstone.ParentDelegate;
import org.eclipse.jface.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The top-level dialog for our ghost comment implementation.
 * 
 * @author Michael Puskas
 */
public class GhostCommentInformationControl extends AbstractInformationControl implements IInformationControlExtension2,
		ParentDelegate, GhostCommentControl.EditorDelegate
{
	private final ITextEditor editor;
	private final Color lightBlue;

	private Composite baseComposite;
	private List<GhostCommentControl> controls;
	private boolean forceClose;

	public GhostCommentInformationControl(Shell parentShell, ITextEditor editor) {
		super(parentShell, false);
		this.editor = editor;

		//Init colors
		lightBlue = new Color(getShell().getDisplay(), 138, 203, 214);
		create();
	}

	private GhostCommentInformationControl(Shell parentShell, GhostCommentInformationControl unenrichedControl) {
		super(parentShell, true);
		this.editor = unenrichedControl.editor;
		forceClose = unenrichedControl.forceClose;

		//Init colors
		lightBlue = new Color(getShell().getDisplay(), 138, 203, 214);
		create();

		//Create the dialog area
		createDialogArea(unenrichedControl.controls.stream().map(GhostCommentInformationControl::recycle));
		unenrichedControl.controls.clear();
	}

	private static Function<ParentDelegate, GhostCommentControl> recycle(GhostCommentControl control) {
		return parentDelegate -> {
			control.setParentDelegate(parentDelegate);
			return control;
		};
	}

	@Override
	public void setLocation(Point location) {
		if (!isResizable()) {
			ITextOperationTarget target = editor.getAdapter(ITextOperationTarget.class);
			ITextViewer viewer = (ITextViewer)target;
			ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
			StyledText st = viewer.getTextWidget();

			Point initialLocation = location;
			OptionalInt maxHighlightedX = controls.stream().flatMap(c -> c.getHightlightPositions().stream())
					.map(position -> position.getOffset() + position.getLength())
					.map(extension5::modelOffset2WidgetOffset)
					.map(offset -> {
						Point loc = st.getLocationAtOffset(offset);
						return new Point(loc.x, loc.y + st.getLineHeight(offset));
					})
					.map(st::toDisplay)
					.filter(loc -> loc.y >= initialLocation.y)
					.mapToInt(loc -> loc.x)
					.max();

			if (maxHighlightedX.isPresent()) {
				int x = maxHighlightedX.getAsInt();
				if (x > location.x) location = new Point(x + 8, location.y);
			}
		}
		super.setLocation(location);
	}

	@Override
	protected void createContent(Composite parent) {
		//Create the top level composite
		baseComposite = new Composite(parent, SWT.NONE);
		//baseComposite.setLayoutData(new FillData(SWT.FILL, SWT.FILL, true, true));
		baseComposite.setBackground(lightBlue);

		//Set up the layout
		GridLayout layout = new GridLayout();
		baseComposite.setLayout(layout);
	}

	@Override
	public boolean hasContents() {
		return true;
	}

	@Override
	public void setInput(Object input) {
		if (controls != null || !(input instanceof GhostCommentInputCreator)) return;

		GhostCommentInputCreator inputCreator = (GhostCommentInputCreator) input;
		Stream<Function<ParentDelegate, GhostCommentControl>> controlCreators =
				inputCreator.createControls(this);

		//Create the dialog area
		createDialogArea(controlCreators);
		controls.get(controls.size() - 1).addBrowser();
		controls.get(0).focus();
	}

	private void createDialogArea(Stream<Function<ParentDelegate, GhostCommentControl>> controlCreators)
	{
		controls = controlCreators.map(controlCreator -> {
			GhostCommentControl control = controlCreator.apply(this);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return control;
		}).collect(Collectors.toList());
	}

	@Override
	public Composite getParent() {
		return baseComposite;
	}

	@Override
	public void onFocus(Composite sender) {
		controls.stream().filter(c -> c != sender).forEach(GhostCommentControl::unfocus);
	}

	@Override
	public boolean close() {
		forceClose = true;
		setVisible(false);
		return isVisible();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			Shell shell = getShell();
			shell.layout(false);
			shell.pack();

			pushBackOnDisplay(shell);
		}

		super.setVisible(visible && !forceClose);

		if (!visible) {
			handleClose();
		}
	}

	public static void pushBackOnDisplay(Shell shell) {
		Point location = shell.getLocation();
		Rectangle displayBounds = shell.getDisplay().getBounds();
		Point shellSize = shell.getSize();
		int pushBack = location.x + shellSize.x - displayBounds.width;
		if (pushBack > 0) {
            location.x -= pushBack;
            shell.setLocation(location);
        }
	}

	@Override
	public void dispose() {
		handleClose();
		
		//Dispose of the colors
		lightBlue.dispose();
		super.dispose();
	}

	private void handleClose() {
		if (controls != null) {
			// Remove the highlights
			controls.forEach(GhostCommentControl::onClose);
			controls.clear();
		}
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return parent -> new GhostCommentInformationControl(parent, this);
	}

	@Override
	public boolean allowAcceptComment() {
		return true;
	}

	@Override
	public void acceptedComment(CommentInfo commentInfo) {
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
}
