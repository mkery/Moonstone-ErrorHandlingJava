package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.markers.MyMarkerFactory;
import moonstone.AbstractComposite;
import moonstone.EditorUtility;
import moonstone.ParentDelegate;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.net.URL;
import java.util.List;

/** The top-level dialog for our custom quickfix implementation.
 * 
 * @author Michael Puskas
 */
public class QuickFixControl extends AbstractComposite
{
	private final ITextEditor editor;
	protected final IAnnotationModel model;

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

	public QuickFixControl(ITextEditor editor
			, MarkerAnnotation markerAnno, ParentDelegate parentDelegate)
	{
		//Set the shell style
		super(SWT.NONE, parentDelegate);
		setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		setLayout(layout);

		this.editor = editor;
		//Initialize the annotation model
		IDocumentProvider idp = editor.getDocumentProvider();
		model = idp.getAnnotationModel(editor.getEditorInput());

		//Remove potentially stale highlights
		MyMarkerFactory.removeStaleHighlights(model);

		this.markerAnno = markerAnno;
		this.clickedMarker = markerAnno.getMarker();

		initTryParent();
		
		//Init colors
		lightBlue = new Color(getDisplay(), 138, 203, 214);
		highlightBlue = new Color(getDisplay(), 219, 234, 241);
		
		//Init images
		URL iconURL = QuickFixControl.class.getResource("/icon/moonstone.png");
		moonstone = (Image) ImageDescriptor.createFromURL(iconURL).createResource(getDisplay());

		//Get the resolutions for the marker
		resGenerator = new ResolutionGenerator();
		problemDescription = resGenerator.getDescription(clickedMarker);

		if (selection != null) {
			resArr = tryParent != null
					? resGenerator.getResolutions(model, markerAnno, tryParent, selection)
					: resGenerator.getResolutions(model, markerAnno, selection);
		} else resArr = null;
	}

	public void createDialogArea() {
		//Create the dialog area
		Composite left = new Composite(this, SWT.NONE);
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout leftLayout = new GridLayout();
		leftLayout.marginWidth = 0; //  = leftLayout.marginHeight
		left.setLayout(leftLayout);
		left.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		Composite right = new Composite(this, SWT.NONE);
		right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout rightLayout = new GridLayout();
		//rightLayout.marginWidth = rightLayout.marginHeight = 0;
		right.setLayout(rightLayout);
		right.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		//Create the right-side text box
		createTextBox(right);

		//Create the top label
		createLabel(left);

		//Create the scrollable composite container with buttons enclosed
		createTopScrollArea(left);
		createBottomScrollArea(left);
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

								parentDelegate.close(); //Close the dialog
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

				parentDelegate.close(); //Close the dialog
			}
		});

	    return child;
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

	void onClose() {}

	@Override
	public void dispose() {
		moonstone.dispose();
		lightBlue.dispose();
		super.dispose();
	}
}
