package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.ghostcomment.GhostCommentControl;
import edu.cmu.moonstone.markers.MarkerDef;
import moonstone.ParentDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import static edu.cmu.moonstone.ghostcomment.GhostCommentInformationControl.pushBackOnDisplay;

/** The top-level dialog for our ghost comment implementation.
 * 
 * @author Michael Puskas
 */
public class QuickFixInformationControl extends AbstractInformationControl implements IInformationControlExtension2,
		ParentDelegate
{
	private final ITextEditor editor;
	private final Color lightBlue;

	private Composite baseComposite;
	private QuickFixControl control;
	private boolean forceClose;

	public QuickFixInformationControl(Shell parentShell, ITextEditor editor) {
		super(parentShell, false);
		this.editor = editor;

		//Init colors
		lightBlue = new Color(getShell().getDisplay(), 138, 203, 214);
		create();
	}

	private QuickFixInformationControl(Shell parentShell, QuickFixInformationControl unenrichedControl) {
		super(parentShell, true);
		this.editor = unenrichedControl.editor;
		forceClose = unenrichedControl.forceClose;

		//Init colors
		lightBlue = new Color(getShell().getDisplay(), 138, 203, 214);
		create();

		//Create the dialog area
		QuickFixControl control = unenrichedControl.control;
		if (control != null) {
			control.setParentDelegate(this);
			createDialogArea(control);
			unenrichedControl.control = null;
		}
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
		if (control != null || !(input instanceof MarkerAnnotation)) return;
		MarkerAnnotation annotation = (MarkerAnnotation) input;

		//Create the dialog area
		QuickFixControl control;
		switch (0) {
			case 0:
				try {
					if (annotation.getMarker().isSubtypeOf(MarkerDef.BAD_CATCH_MARKER)) {
						control = new BadCatchControl(editor, annotation, this);
						break;
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			default:
				control = new QuickFixControl(editor, annotation, this);
		}
		
		control.createDialogArea();
		createDialogArea(control);
	}

	private void createDialogArea(QuickFixControl control)
	{
		control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.control = control;
	}

	@Override
	public Composite getParent() {
		return baseComposite;
	}

	@Override
	public void onFocus(Composite sender) {
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

		if (!visible && control != null) {
			handleClose();
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
		//Remove the highlights
		if (control != null)
			control.onClose();
		control = null;
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return parent -> new QuickFixInformationControl(parent, this);
	}
}
