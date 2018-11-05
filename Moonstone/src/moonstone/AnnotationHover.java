package moonstone;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.quickfix.QuickFixInformationControl;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.text.java.hover.AbstractAnnotationHover;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * This is how you implement a text hover. Anything could go in here. It's not currently used. TODO
 * @author mkery
 *
 */
@SuppressWarnings("restriction")
public class AnnotationHover extends AbstractAnnotationHover
{
	public AnnotationHover()
	{
		super(false);
	}

    /* @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 * @since 3.4
	 */
	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		AnnotationInfo annotationInfo = (AnnotationInfo) super.getHoverInfo2(textViewer, hoverRegion);
		if (annotationInfo == null) return null;
		try {
			return annotationInfo.annotation instanceof MarkerAnnotation 
					&& ((MarkerAnnotation)annotationInfo.annotation).getMarker().isSubtypeOf(MarkerDef.MARKER)
						? annotationInfo.annotation 
						: null;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ITextEditor getEditor() {
		return super.getEditor().getAdapter(ITextEditor.class);
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.0
	 */
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return parentShell -> new QuickFixInformationControl(parentShell, getEditor());
	}
}