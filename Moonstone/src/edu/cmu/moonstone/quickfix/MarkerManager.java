package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.markers.MarkerDef;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.stream.StreamSupport;

/** Handles a custom implementation of eclipse-like quickfix functionality
 * 
 * @author Michael Puskas
 */
public class MarkerManager 
{
	public static boolean windowOpen; //We only ever want 1 window open
	private final ITextEditor editor;
	private final ITextViewer viewer;
	private final IDocument doc;
	private final IAnnotationModel model;

	private Point anchorPoint; //A point just under the bottom left of the annotation, if one of our markers is found

	public MarkerManager(IEditorPart editorPart)
	{
		editor = editorPart.getAdapter(ITextEditor.class);
		viewer = (ITextViewer) editorPart.getAdapter(ITextOperationTarget.class);

		//Initialize the annotation model
		IDocumentProvider idp = editor.getDocumentProvider();
		doc = idp.getDocument(editor.getEditorInput());
		model = idp.getAnnotationModel(editor.getEditorInput());
	}
	
	/** Adds a listener that handles the custom quickfix functionality for Moonstone markers
	 * 
	 */
	public void addMarkerListener()
	{
		Control rulerControl = editor.getAdapter(IVerticalRulerInfo.class).getControl();
		rulerControl.addMouseListener(new MouseAdapter() //Checks to see if a marker was clicked on
		{
			@Override
			public void mouseDown(MouseEvent e) {
				MarkerAnnotation clickedAnnotation;
				if (!windowOpen && (clickedAnnotation = confirmHit(e, false)) != null) //If one of our markers was clicked
					openQuickFixDialog(clickedAnnotation, false);
			}
		});
	}

	private void openQuickFixDialog(MarkerAnnotation annotation, boolean isHover) {
		//Initialize the quickfix dialog
		QuickFixDialog qfDialog;
		try
        {
			Shell activeShell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
			if(annotation.getMarker().isSubtypeOf(MarkerDef.BAD_CATCH_MARKER)) //If it's a bad catch
				qfDialog = new BadCatchDialog(activeShell, editor
						, anchorPoint,
						annotation);
            else //Else, open a standard dialog
				qfDialog = new QuickFixDialog(activeShell, editor
						, anchorPoint,
						annotation);
        }
        catch(CoreException e)
        {
            throw new RuntimeException(e);
        }

		//Open the quickfix dialog
		windowOpen = true;
		if (isHover) qfDialog.openNoFocus();
		else qfDialog.open();
		qfDialog.initCloseListener(viewer.getTextWidget(), isHover);
	}

	/** Checks if the user is hovering over one of our QuickFix annotations
	 * 
	 * @param ev The MouseEvent from the user's hover
	 * @return Our marker that was hovered on, or null if none of our markers were hovered
	 */
	private MarkerAnnotation confirmHit(MouseEvent ev, boolean hitText)
	{
		ITextViewerExtension5 extension5 = (ITextViewerExtension5) viewer;
		StyledText st = viewer.getTextWidget();

		//If the annotation belongs to a marker, cast it as such
		Iterable<MarkerAnnotation> markerAnnotations = ofType(MarkerAnnotation.class, model::getAnnotationIterator);

		/* Check all of the annotations on the current model, looking for Marker annotations */
		for (MarkerAnnotation annotation: markerAnnotations) {
			//If we've found a marker annotation, check if the mouse is hovering over it
			try
			{
				//If the ID is one of ours
				if (!annotation.getMarker().isSubtypeOf(MarkerDef.MARKER)) continue;

				//Find the position of the current annotation
				Position position = model.getPosition(annotation);

				//Relative location takes hidden imports into account
				int widgetOffset = extension5.modelOffset2WidgetOffset(position.offset);
				if (widgetOffset < 0) continue;

				//Check if the hover was within the hitbox
				if (hitText) {
					int widgetEndOffset = extension5.modelOffset2WidgetOffset(position.offset + position.length - 1);
					if (widgetEndOffset < 0) continue;

					//Hitbox
					boolean miss = true;
					int lineCount = st.getLineCount();
					for (int startLine = st.getLineAtOffset(widgetOffset),
						       endLine = st.getLineAtOffset(widgetEndOffset); startLine <= endLine; startLine++) {
						int startOfLine = st.getOffsetAtLine(startLine);
						int nextLine = startLine + 1;
						int endOfLine =
							(nextLine < lineCount
								? st.getOffsetAtLine(nextLine)
								: st.getCharCount()
							) - 1;

						Rectangle lineBox;
						try {
							lineBox = st.getTextBounds(Math.max(startOfLine,  widgetOffset),
													   Math.min(endOfLine, widgetEndOffset));
						} catch (IllegalArgumentException ignored) {
							break;
						}

						if (lineBox.contains(ev.x, ev.y)) {
							miss = false;
							break;
						}
					}
					if (miss) continue;
				} else {
					Point markerPoint = st.getLocationAtOffset(widgetOffset);
					if (ev.y < markerPoint.y || markerPoint.y + st.getLineHeight() <= ev.y) continue;
				}

				//Calculate the location that the dialog box will be drawing to and initialize the anchorPoint.
				anchorPoint = st.toDisplay(ev.x, ev.y);
				return annotation;
			}
			catch(CoreException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		return null;
	}

	public static <I,T extends I> Iterable<T> ofType(Class<T> out, Iterable<I> annotations) {
		return StreamSupport.stream(annotations.spliterator(), false)
				.filter(out::isInstance).map(out::cast)::iterator;
	}

	/** Takes a marker's ID String and chops off the last two bits so you can compare what grandparent marker it comes from.
	 * 
	 * @param childID
	 * @return parentID
	 */
	public static String getGrandParentID(String childID)
	{
		childID = childID.substring(0, childID.lastIndexOf("."));
		return childID.substring(0, childID.lastIndexOf('.'));
	}

	/** Takes a marker's ID String and chops off the last bit so you can compare what parent marker it comes from.
	 * 
	 * @param childID
	 * @return grandParentID
	 */
	public static String getParentID(String childID)
	{
		return childID.substring(0, childID.lastIndexOf('.'));
	}
	
	/** Takes a marker's fully qualified ID and returns the ID of the general type of marker it is
	 * 
	 * @param ID Ex: edu.cmu.moonstone.marker.badcatch.caughtexception
	 * @return simpleID Ex: badcatch - empty string if the ID doesn't match one of our markers
	 */
	public static String getGeneralID(String ID)
	{
		String parentID = getParentID(ID);
		
		if(parentID.equals(MarkerDef.MARKER)) //If the marker is already a general type, return its simple ID
			return ID.substring(ID.lastIndexOf('.') + 1);
		else //Else the marker is a child type, return its parent's simple ID 
			return parentID.substring(parentID.lastIndexOf('.') + 1);
	}
}