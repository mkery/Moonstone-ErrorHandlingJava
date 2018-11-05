package edu.cmu.moonstone.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.IMarkerUpdater;

public class MarkerUpdater implements IMarkerUpdater {
	/*
	*Returns the attributes for which this updater is responsible.
	*If the result is null, the updater assumes responsibility for any attributes.
	*/
	@Override
	public String[] getAttribute() {
	      return null;
	}

	/*
	 * Returns the marker type, but this isn't currently used.
	 * If we use this, we'll need to include the other marker types
	 */
	@Override
	public String getMarkerType() {
	      return MarkerDef.MARKER;
	}

	/*
	 * This updates the position of the marker so that it stays relative to the text's position.
	 * For instance, if a newline is created in the file before the marker, this will be called 
	 * 
	 * @param marker - The marker that needs its position updated
	 * @param doc - the user's code document we're in, such as MyClass.java
	 * @param position - The position within the document that the marker needs to be moved to
	 */
	@Override
	public boolean updateMarker(IMarker marker, IDocument doc, Position position) {
	      try {
	            int start = position.getOffset();
	            int end = position.getOffset() + position.getLength();
	            marker.setAttribute(IMarker.CHAR_START, start);
	            marker.setAttribute(IMarker.CHAR_END, end);
	            return true;
	      } catch (CoreException e) {
	            return false;
	      }
	}
}
