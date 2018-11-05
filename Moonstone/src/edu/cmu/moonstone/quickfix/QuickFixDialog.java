package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.markers.MyMarkerFactory;
import moonstone.EditorUtility;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.net.URL;
import java.util.List;

import static edu.cmu.moonstone.ghostcomment.GhostCommentInformationControl.pushBackOnDisplay;

/** The top-level dialog for our custom quickfix implementation.
 * 
 * @author Michael Puskas
 */
public class QuickFixDialog extends Dialog
{
	private final ITextEditor editor;
	protected final IAnnotationModel model;
	private Point anchorPoint; //The point to anchor the top left of the dialog at (Just below the annotation)

	private Object closeListener;
	protected final Color lightBlue;
	private final Color highlightBlue;
	
	protected final Image moonstone;

	protected final ResolutionGenerator resGenerator; //A generator for the resolutions
	protected final IMarker clickedMarker; //The marker that the user clicked or hovered on
	protected final MarkerAnnotation markerAnno; //The annotation tied to the clicked marker
	protected final List<ResolutionInfo> resArr; //Holds the appropriate resolutions
	protected final Description problemDescription; //The description that gets displayed in the right StyledText textbox
	
	protected TryStatement tryParent;
	private ASTNode selection;

	public QuickFixDialog(Shell parentShell, ITextEditor editor
			, Point anchorPoint, MarkerAnnotation markerAnno)
	{
		//Set the shell style
		super(parentShell);
		setShellStyle(SWT.TOOL | SWT.ON_TOP);
		setBlockOnOpen(false);

		this.editor = editor;
		//Initialize the annotation model
		IDocumentProvider idp = editor.getDocumentProvider();
		model = idp.getAnnotationModel(editor.getEditorInput());

		//Remove potentially stale highlights
		MyMarkerFactory.removeStaleHighlights(model);

		this.anchorPoint = anchorPoint;
		this.markerAnno = markerAnno;
		this.clickedMarker = markerAnno.getMarker();

		initTryParent();
		
		//Init colors
		lightBlue = new Color(parentShell.getDisplay(), 138, 203, 214);
		highlightBlue = new Color(parentShell.getDisplay(), 219, 234, 241);
		
		//Init images
		URL iconURL = QuickFixDialog.class.getResource("/icon/moonstone.png");
		moonstone = (Image) ImageDescriptor.createFromURL(iconURL).createResource(parentShell.getDisplay());

		//Get the resolutions for the marker
		resGenerator = new ResolutionGenerator();
		problemDescription = resGenerator.getDescription(clickedMarker);

		if (selection != null) {
			resArr = tryParent != null
					? resGenerator.getResolutions(model, markerAnno, tryParent, selection)
					: resGenerator.getResolutions(model, markerAnno, selection);
		} else resArr = null;
	}

	@Override
	public int open() {
		create();
		Shell shell = getShell();
		shell.layout(false);
		shell.pack();
		shell.layout(true);

		pushBackOnDisplay(shell);
		return super.open();
	}

	public int openNoFocus() {
		create();
		Shell shell = getShell();
		shell.layout(false);
		shell.pack();
		shell.layout(true);

		pushBackOnDisplay(shell);
		shell.setVisible(true);
		return Window.OK;
	}

	@Override
	protected Control createContents(Composite parent) 
	{
		//Create the top level composite
		Composite baseComposite = new Composite(parent, SWT.NONE);
		baseComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		baseComposite.setBackground(lightBlue);

		//Initialize the dialog units
		applyDialogFont(baseComposite);
		initializeDialogUnits(baseComposite);

		//Set up the layout
		GridLayout layout = new GridLayout(2, false);
		baseComposite.setLayout(layout);

		//Create the dialog area
		dialogArea = createDialogArea(baseComposite);

		return baseComposite;
	}

	@Override
	protected Control createDialogArea(Composite container)
	{
		Composite left = new Composite(container, SWT.NONE);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout leftLayout = new GridLayout();
		leftLayout.marginWidth = 0; //  = leftLayout.marginHeight
		left.setLayout(leftLayout);
		left.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		Composite right = new Composite(container, SWT.NONE);
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout rightLayout = new GridLayout();
		//rightLayout.marginWidth = rightLayout.marginHeight = 0;
		right.setLayout(rightLayout);
		right.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		//Create the right-side text box
		createTextBox(right);

		//Create the top label
		createLabel(left);

		//Create the scrollable composite container with buttons enclosed
		createTopScrollArea(left);
		createBottomScrollArea(left);
		
		return container;
	}
	
	/** Creates the scroll areas for the labels (bottom left). Also creates a canvas to draw a border above the scroll area
	 * 
	 * @param container 
	 */
	protected Composite createBottomScrollArea(Composite container)
	{
		return container;
	}
	
	/** Creates the text box and button on the right side of the dialog
	 * 
	 * @param container
	 * @return Returns the text box
	 */
	protected StyledText createTextBox(Composite container)
	{
		//Create the text box on the right 
		StyledText textBox = new StyledText(container, SWT.WRAP | SWT.NO_FOCUS)/* { //SWT.V_SCROLL | 
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return new Point(0,0);
			} 
		}*/;
		GridData textBoxLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		textBoxLayout.widthHint = 350;
		textBox.setLayoutData(textBoxLayout);
		textBox.setAlwaysShowScrollBars(false);

	    //Add the text
		String title = problemDescription.title;
		String body = problemDescription.body;
		textBox.append(title + body);
		
		//StyleRange changes the style on a certain range of text. In this case, bolding the title.
		textBox.setStyleRange(new StyleRange(0, title.length(), null, null, SWT.BOLD));
		
		//Can't edit or click into the text box
		textBox.setEditable(false);

		//Add button for linking to the webpage
		/*Button moreButton = new Button(container, SWT.PUSH);
		moreButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		moreButton.setText("Explanation");

	    //Make the webpage button link to the site
		moreButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(problemDescription.url); //Why is this so easy when everything else is so hard?

				close(); //Close the dialog
			}
		});*/

	    return textBox;
	}

	/** Creates the label in the top left
	 * 
	 * @param container
	 * @return Returns the label
	 */
	protected CLabel createLabel(Composite container)
	{
		//Create the top left label
		CLabel label = new CLabel(container, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		label.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		//Set the content of the label
		label.setText(problemDescription.header);
		label.setImage(moonstone);
		
		return label;
	}

	/** Draws the label scroll area in the middle left of the dialog
	 * 
	 * @param container
	 * @return Returns the label
	 */
	protected Composite createTopScrollArea(Composite container)
	{
		// Create a child composite to hold the controls
		Composite child = new Composite(container, SWT.NONE);
		child.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		child.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		child.setLayout(new FillLayout(SWT.VERTICAL));

		//Create the buttons and add them to the child composite
		Image greenArrow = JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		if(resArr != null)
		{
			for(ResolutionInfo resInfo : resArr) //For each resolution that we have available
			{
				//If it's a suggestion, add a greyed out label
				if(resInfo.getResolution() == null)
				{
					new QuickFixSuggestionLabel(child, resInfo.getProposition());
				}
				else //Else it isn't a suggestion, add a resolution button
				{
					new QuickFixLabel(child, resInfo.getProposition(), greenArrow, highlightBlue).getLabel().addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseUp(MouseEvent event) //Resolve, close window, delete marker
						{
							super.mouseUp(event);

							if(event.getSource() instanceof CLabel) 
							{
								//Create a quickFix object and run the selected resolution

								try {
									QuickFix quickFix = new QuickFix(editor, clickedMarker, markerAnno, resInfo.getResolution(), selection);
									quickFix.run();
								} catch (BadLocationException e) {
									throw new RuntimeException(e);
								}

								close(); //Close the dialog
							}
						}
					});
				}
			}
		}
		
		//Add the ignore option
		CLabel qfLabel = new QuickFixLabel(child, "Ignore all in method", greenArrow, highlightBlue).getLabel();
		qfLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent event) //Resolve, close window, delete marker
			{
				super.mouseUp(event);

				//Create a quickFix object and run the selected resolution
				try {
					QuickFix quickFix = new QuickFix(editor, clickedMarker, markerAnno, Resolution.IGNORE, selection);
					quickFix.run();
				} catch (BadLocationException ignored) {
				}

				close(); //Close the dialog
			}
		});

	    return child;
	}
	
	/** Initializes the closeListener
	 * Checks for what type of listener to add and saves the added listener to closeListener
	 * 
	 * @param editorStyledText The StyledText to get the line height from
	 * @param isHover True if the user is hovering to prompt the dialog, false if the user clicked a marker
	 */
	public void initCloseListener(StyledText editorStyledText, boolean isHover)
	{
		if(isHover) //If the user hovered, add a listener for mouse movement
		{
			closeListener = new MouseMoveListener()
			{
				//The extra space to add to encompass the user's original cursor position
				private final int lineHeight = editorStyledText.getLineHeight() + 1;

				@Override
				public void mouseMove(MouseEvent e) 
				{
					Point mousePoint = new Point(e.x, e.y);
					mousePoint = ((Control) e.getSource()).toDisplay(mousePoint);

					Point size = getShell().getSize();
					Rectangle expandedBoundingRect = new Rectangle(
							anchorPoint.x,
							(anchorPoint.y - lineHeight),
							size.x,
							(size.y + lineHeight));

					//If the flag is active and the user moves their mouse outside of the bounds, close the dialog
					if(!expandedBoundingRect.contains(mousePoint.x, mousePoint.y))
						close();
				}
			};
		}
		else //Else the user clicked the marker, add a listener for clicking the dialog away
		{
			closeListener = new MouseAdapter()
			{
		        @Override
		        public void mouseDown(MouseEvent e) 
		        {
		        	if(!e.getSource().toString().contains("AnnotationRulerColumn")) //To kill initial click
						close(); //Close the dialog
		        }
			};
		}

		//Save the closeListener for later adding/removing
		addCloseListener(getParentShell());
	}

	/** Recursively adds MouseListeners to all controls to check when the user clicks outside of the dialog
	 * 
	 * @param currentControl
	 */
	private void addCloseListener(Control currentControl)
	{
		if(closeListener instanceof MouseMoveListener)
			currentControl.addMouseMoveListener((MouseMoveListener) closeListener);
		else //Else, it's a MouseListener
			currentControl.addMouseListener((MouseListener) closeListener);

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
		if(closeListener instanceof MouseMoveListener)
			currentControl.removeMouseMoveListener((MouseMoveListener) closeListener);
		else //Else, it's a MouseListener
			currentControl.removeMouseListener((MouseListener) closeListener);

	    if (currentControl instanceof Composite) 
	        for (final Control cc : ((Composite) currentControl).getChildren()) 
	            remCloseListener(cc);
	}
	
	/** Uses the current editor from StartupUtility to find what TryStatement the marker is attached to
	 * 
	 */
	private void initTryParent()
	{
		//Get the appropriate compilation unit
		IEditorInput input = editor.getEditorInput();
		ICompilationUnit cu = (ICompilationUnit) JavaUI.getEditorInputJavaElement(input);

		//Use the annotation's starting position to find the try parent
		Position position = model.getPosition(markerAnno);
		if (position == null) return;

		//Set QuickFixDialog's member variable to the outermost try statement
		selection = EditorUtility.getSelectedNode(cu, position.offset, position.length);
		tryParent = EditorUtility.inTryStatement(selection);
	}
	
	@Override
	protected Point getInitialLocation(Point initialSize)
	{
		return anchorPoint;
	}
	
	@Override
	public boolean close()
	{
		remCloseListener(getParentShell());
		MarkerManager.windowOpen = false;

		moonstone.dispose();
		lightBlue.dispose();
		
		return super.close();
	}
}
