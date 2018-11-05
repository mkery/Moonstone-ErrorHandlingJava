package edu.cmu.moonstone.ghostcomment;

import moonstone.ParentDelegate;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.cmu.moonstone.ghostcomment.GhostCommentInformationControl.pushBackOnDisplay;

/** The top-level dialog for our ghost comment implementation.
 * 
 * @author Michael Puskas
 */
public class GhostCommentDialog extends Dialog implements ParentDelegate
{
	private final Stream<Function<ParentDelegate, GhostCommentControl>> controlCreators;
	private final Point anchorPoint; //The point to anchor the top left of the dialog at (Just below the annotation)
	private final Color lightBlue;
	private final Runnable onClose;

	private Composite baseComposite;
	private MouseMoveListener closeListener;
	private List<GhostCommentControl> controls;

	public GhostCommentDialog(Shell parentShell, Point anchorPoint,
							  Stream<Function<ParentDelegate, GhostCommentControl>> controlCreators, Runnable onClose)
	{
		//Set the shell style
		super(parentShell);
		setShellStyle(SWT.TOOL | SWT.ON_TOP);
		setBlockOnOpen(false);

		this.controlCreators = controlCreators;
		this.anchorPoint = anchorPoint;
		this.onClose = onClose;

		lightBlue = new Color(parentShell.getDisplay(), 138, 203, 214);
	}

	@Override
	public int open() {
		create();
		Shell shell = getShell();
		shell.layout(false);
		shell.pack();

		pushBackOnDisplay(shell);
		shell.setVisible(true);
		return Window.OK;
	}

	@Override
	protected Control createContents(Composite parent)
	{
		//Create the top level composite
		baseComposite = new Composite(parent, SWT.NONE);
		baseComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		baseComposite.setBackground(lightBlue);

		//Initialize the dialog units
		applyDialogFont(baseComposite);
		initializeDialogUnits(baseComposite);

		//Set up the layout
		GridLayout layout = new GridLayout();
		baseComposite.setLayout(layout);

		//Create the dialog area
		dialogArea = createDialogArea(baseComposite);

		return baseComposite;
	}

	@Override
	protected Control createDialogArea(Composite container)
	{
		controls = controlCreators.map(controlCreator -> {
			GhostCommentControl control = controlCreator.apply(this);
			control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			return control;
		}).collect(Collectors.toList());
		
		controls.get(controls.size() - 1).addBrowser();
		controls.get(0).focus();

		return container;
	}

	@Override
	public Composite getParent() {
		return baseComposite;
	}

	@Override
	public void onFocus(Composite sender) {
		
		controls.stream().filter(c -> c != sender).forEach(GhostCommentControl::unfocus);
	}

	/** Initializes the closeListener
	 * 
	 * @param editorStyledText The StyledText to get the line height from
	 */
	public void initCloseListener(StyledText editorStyledText)
	{
		closeListener = new MouseMoveListener()
		{
			Point dialogSize = getShell().getSize();
			//The extra space to add to encompass the user's original cursor position
			private final int lineHeight = editorStyledText.getLineHeight() + 1; 
			Rectangle expandedBoundingRect = new Rectangle(
					anchorPoint.x,
					(anchorPoint.y - lineHeight),
					dialogSize.x,
					(dialogSize.y + lineHeight));

			@Override
			public void mouseMove(MouseEvent e) 
			{
				Point mousePoint = new Point(e.x, e.y);
				mousePoint = ((Control) e.getSource()).toDisplay(mousePoint);

				//If the flag is active and the user moves their mouse outside of the bounds, close the dialog
				if(!expandedBoundingRect.contains(mousePoint.x, mousePoint.y))
					close();
			}
		};

		//Save the closeListener for later adding/removing
		addCloseListener(this.getParentShell());
	}

	/** Recursively adds MouseMoveListeners to all controls to check when the user clicks outside of the dialog
	 * 
	 * @param currentControl
	 */
	private void addCloseListener(Control currentControl)
	{
		currentControl.addMouseMoveListener(closeListener);

	    if (currentControl instanceof Composite) 
	        for (final Control cc : ((Composite) currentControl).getChildren()) 
	            addCloseListener(cc);
	}

	/** Recursively removes the specifies MouseListener from the specified control
	 * 
	 * @param currentControl
	 */
	private void remCloseListener(Control currentControl)
	{
		currentControl.removeMouseMoveListener(closeListener);

	    if (currentControl instanceof Composite) 
	        for (final Control cc : ((Composite) currentControl).getChildren()) 
	            remCloseListener(cc);
	}

	@Override
	protected Point getInitialLocation(Point initialSize)
	{
		return anchorPoint;
	}
	
	@Override
	public boolean close()
	{
		remCloseListener(this.getParentShell());
		onClose.run();
		controls.forEach(GhostCommentControl::onClose);

		//Dispose of the colors
		lightBlue.dispose();

		return super.close();
	}
}
