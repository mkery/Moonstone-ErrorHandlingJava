package edu.cmu.moonstone.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import java.util.*;
import java.util.stream.StreamSupport;

public class MyMarkerFactory {
	static final boolean debug = true;
	
	//Annotation ID
	public static final String QFMARKERANNOTATION = "edu.cmu.moonstone.qfannotation";

	abstract static class Marker {
		public abstract String getMarkerType();
		public abstract IRegion getRegion();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof Marker)) return false;
			Marker that = (Marker) o;
			return Objects.equals(getRegion(), that.getRegion()) &&
					Objects.equals(getMarkerType(), that.getMarkerType());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getMarkerType(), getRegion());
		}

		public void delete() {}
	}
	class MarkerWrapper extends Marker {
		public final IMarker marker;

		MarkerWrapper(IMarker marker) {
			this.marker = marker;
		}

		@Override
		public String getMarkerType() {
			try {
				return marker.getType();
			} catch (CoreException e) {
				return null;
			}
		}

		@Override
		public IRegion getRegion() {
			int offset = marker.getAttribute(IMarker.CHAR_START, -1);
			int end = marker.getAttribute(IMarker.CHAR_END, -1);
			return new Region(offset, end - offset);
		}

		@Override
		public void delete() {
			try {
				marker.delete();
			} catch (CoreException ignored) {
			}
		}
	}
	class MarkerPlaceholder extends Marker {
		private final String markerType;
		private final IRegion region;

		MarkerPlaceholder(String markerType, IRegion region) {
			this.markerType = markerType;
			this.region = region;
		}

		@Override
		public String getMarkerType() {
			return markerType;
		}

		@Override
		public IRegion getRegion() {
			return region;
		}
	}

	private Set<Marker> activeMarkers = new HashSet<>();
	private Set<Marker> newMarkers = new HashSet<>();
	private IResource activeResource;

	private void validateActiveResource(IResource res) {
		if (activeResource != res)
			throw new AssertionError("Called with other than active resource");
	}

	public void beginUpdate(IResource res, ASTNode node) {
		if (activeResource != null)
			throw new AssertionError("Called beginUpdate twice, before endUpdate");

		findMarkers(activeResource = res).stream()
				.map(MarkerWrapper::new)
				.filter(markerWrapper -> node.getStartPosition() <= markerWrapper.marker.getAttribute(IMarker.CHAR_START, -1)
                        && markerWrapper.marker.getAttribute(IMarker.CHAR_END, -1) <= node.getStartPosition() + node.getLength())
				.forEach(activeMarkers::add);
	}

	public void cancelUpdate(IResource res) {
		validateActiveResource(res);
		activeMarkers.clear();
		newMarkers.forEach(Marker::delete);
		newMarkers.clear();
	}

	public void endUpdate(IResource res) {
		validateActiveResource(res);
		activeMarkers.forEach(Marker::delete);
		activeMarkers.clear();
		activeResource = null;
	}

	/**
	 * Sets up and creates a marker at the input position with the input message and type
	 * 
	 * @param res - The file the marker should be created in
	 * @param position - The position within the document that the marker needs to be moved to
	 * @param message - The message to set for the marker, displayed when it's hovered over
	 * @param markerType - The type of marker to create, defined above (and in related files)
	 */
	public IMarker createMarker(IResource res, Region position, String message, String markerType)
    throws CoreException 
	{
		validateActiveResource(res);

		if (activeMarkers.remove(new MarkerPlaceholder(markerType, position))) {
			if (debug) System.out.println("Reusing marker " + markerType + " at offset " + position.getOffset());
			return null;
		} else if (debug) System.out.println("Not reusing marker " + markerType + " at offset " + position.getOffset());
		
		//note: you use the id that is defined in your plugin.xml
		IMarker marker = res.createMarker(markerType);
		marker.setAttribute(IMarker.MESSAGE, message);
		//compute and set char start and char end
		int start = position.getOffset();
		int end = start + position.getLength();
		marker.setAttribute(IMarker.CHAR_START, start);
		marker.setAttribute(IMarker.CHAR_END, end);

		newMarkers.add(new MarkerWrapper(marker));
		return marker;
    }

	/**
	 * Returns a list of a resources markers
	 * 
	 * @param resource - The file the marker(s) are in
	 */
	public static List<IMarker> findMarkers(IResource resource) {
	     try {
	    	 IMarker[] resources = resource.findMarkers(MarkerDef.MARKER, true, IResource.DEPTH_ZERO);
	    	 return Arrays.asList(resources);
	     } catch (CoreException e) {
	    	 return new ArrayList<>();
	    }
	}
	
	/**
	 * Removes the specified marker from the file
	 * 
	 * @param resource - The file the marker is in
	 * @param startPosition - The beginning position the marker is aligned with
	 * @param markerType - The type of marker to look for at the specified position
	 */
	public static void removeMarker(IResource resource, int startPosition, String markerType) {
		try {
			IMarker[] markers = resource.findMarkers(markerType, true, IResource.DEPTH_ZERO);
			for (IMarker marker : markers) {
				if ((int) marker.getAttribute(IMarker.CHAR_START) == startPosition) {
					if (debug) System.out.println("[removeMarker] Removing marker at position: " + startPosition);
					marker.delete();
				}
			}
		} catch (CoreException e) {
			//irony
		}
	}
	
	/*
	 * Returns a list of markers that are linked to the resource or any sub resource of the resource
	 * 
	 * @param resource - The file the marker is in
	 */
	public static List<IMarker> findAllMarkers(IResource resource, String markerType) {
        try {
            return Arrays.asList(resource.findMarkers(markerType, true, IResource.DEPTH_INFINITE));
        } catch (CoreException e) {
            return new ArrayList<>();
        }
    }
	
	/*
	 * Returns the selection of the package explorer
	 */
	public static TreeSelection getTreeSelection() {

		ISelection selection = MyMarkerPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof TreeSelection) {
			return (TreeSelection)selection;
		}
		return null;
	}

	/*
	 * Returns the selection of the package explorer
	 */
	public static TextSelection getTextSelection() {

		ISelection selection = MyMarkerPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof TextSelection) {
			return (TextSelection)selection;
		}
		return null;
	}
	
	/*
	 * Adds an annotation to go along with a marker. 
	 * The annotation spans the provided selection of text.
	 * 
	 * @param marker - The marker to associate the annotation with
	 * @param selection - The seleciton of text/space in the file for the annotation to appear
	 * @param editor - The IDE's text editor used to display the annotation
	 */
	public static void addAnnotation(IMarker marker, ITextSelection selection, ITextEditor editor) {
	      //The DocumentProvider enables to get the document currently loaded in the editor
	      IDocumentProvider idp = editor.getDocumentProvider();

	      //This is the document we want to connect to. This is taken from the current editor input.
	      IDocument document = idp.getDocument(editor.getEditorInput());

	      //The IannotationModel enables to add/remove/change annotation to a Document loaded in an Editor
	      IAnnotationModel iamf = idp.getAnnotationModel(editor.getEditorInput());
	   
	      //Note: The annotation type id specify that you want to create one of your annotations
	      SimpleMarkerAnnotation ma = new SimpleMarkerAnnotation(QFMARKERANNOTATION, marker);
	      ma.setText("Steven test");
	      
	      //Finally add the new annotation to the model
	      iamf.connect(document);
	      iamf.addAnnotation(ma,new Position(selection.getOffset(),selection.getLength()));
	      iamf.disconnect(document);
	      
	      
	      /*
	      try {
			marker.setAttribute(IMarker.SEVERITY, 0);
		      marker.setAttribute(IMarker.CHAR_START, selection.getOffset());
		      marker.setAttribute(IMarker.CHAR_END, selection.getOffset() + selection.getLength());
		      marker.setAttribute(IMarker.LOCATION, "Snake file");
		      marker.setAttribute(IMarker.MESSAGE, "Syntax error");
			} catch (CoreException e) {
				
				System.out.println("000000000000000000 error");
				e.printStackTrace();
			}
	      */
	}

	public static void removeStaleHighlights(IAnnotationModel model) {
		IAnnotationModelExtension modelEx = (IAnnotationModelExtension) model;
		Iterable<Annotation> annotations = model::getAnnotationIterator;
		Annotation[] annotationsToRemove = StreamSupport.stream(annotations.spliterator(), false)
				.filter(annotation -> annotation.getType().equals(MarkerDef.RED_HIGHLIGHT)
						|| annotation.getType().equals(MarkerDef.BLUE_HIGHLIGHT)
						|| annotation.getType().equals(MarkerDef.THROWSITE_HIGHLIGHT)
						|| annotation.getType().equals(MarkerDef.GREY_HIGHLIGHT))
				.toArray(Annotation[]::new);

		for (Annotation annotation : annotationsToRemove) {
			System.err.format("Removing stale %s\n", annotation.getType());
		}
		modelEx.replaceAnnotations(annotationsToRemove, null);
	}
	
}
