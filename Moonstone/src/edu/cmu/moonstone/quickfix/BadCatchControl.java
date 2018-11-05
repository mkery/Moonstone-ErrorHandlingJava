package edu.cmu.moonstone.quickfix;

import moonstone.ParentDelegate;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.List;

/** Extends QuickFixDialog to add functionality for dialogs from bad catch markers.
 * 
 * @author Michael Puskas
 */
public class BadCatchControl extends QuickFixControl
{
	private final List<SituationInfo> situationList;

	public BadCatchControl(ITextEditor editor
			, MarkerAnnotation markerAnno, ParentDelegate parentDelegate)
	{
		super(editor, markerAnno, parentDelegate);
		
		//Get situations for the bottom scroll area
		situationList = resGenerator.getSituations(editor, markerAnno, tryParent);
	}

	/** Creates the scroll areas for the labels (bottom left). Also creates a canvas to draw a border above the scroll area
	 * 
	 * @param container 
	 */
	@Override
	protected Composite createBottomScrollArea(Composite container)
	{
		// Create a child composite to hold the controls
	    Composite child = new Composite(container, SWT.NONE);
	    child.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		child.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		GridLayout gridLayout = new GridLayout();
	    child.setLayout(gridLayout);

	    if (situationList.isEmpty()) return container;
	    
		//Set up the situation label
		Label reachLabel = new Label(child, SWT.NONE);
		reachLabel.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		reachLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
		reachLabel.setText("Exceptional situations known to reach this catch block:");
		reachLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false));
		
		//Add the options
		for(SituationInfo situationInfo : situationList)
			new QuickFixSituationLabel(child, situationInfo.getSituation());
		
		return container;
	}

	@Override
	void onClose() {
		//Use the annotation model for disposing of the situation annotations
		IAnnotationModelExtension modelEx = (IAnnotationModelExtension) model;
		Annotation[] annotations = situationList.stream().map(SituationInfo::getAnnotation).toArray(Annotation[]::new);
		modelEx.replaceAnnotations(annotations, null);
		situationList.clear();
	}
}
