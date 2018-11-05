package moonstone;

import edu.cmu.moonstone.ghostcomment.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.internal.ui.text.java.hover.AbstractJavaEditorTextHover;
import org.eclipse.jface.text.*;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is how you implement a text hover. Anything could go in here. It's not currently used. TODO
 * @author mkery
 *
 */
@SuppressWarnings("restriction")
public class LangHover extends AbstractJavaEditorTextHover
{
	@Override
	public ITextEditor getEditor() {
		IEditorPart editor = super.getEditor();
		return editor != null ? editor.getAdapter(ITextEditor.class) : null;
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return null;
	}
	
    /* @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 * @since 3.4
	 */
	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		ITextEditor editor = getEditor();
		if (editor == null) return null;

		GhostCommentPainter ghostCommentPainter = GhostCommentPainter.getActive(editor);
		if (ghostCommentPainter == null) return null;

		TryStatement tryStatement = ghostCommentPainter.getTryStatement();
		if (tryStatement == null) return null;

		List<Pair<CommentInfo, ExceptionInfo>> infoPairs;
		Pair<CommentInfo, ExceptionInfo> infoPair = ghostCommentPainter.confirmHit(hoverRegion.getOffset());

		if (infoPair != null) {
			infoPairs = Collections.singletonList(infoPair);
		} else {
			List<CommentInfo> commentInfos = ghostCommentPainter.getGhostComments();
			infoPairs = commentInfos.stream()
					.flatMap(commentInfo -> commentInfo.getExceptions().stream()
							.map(e -> Pair.of(commentInfo, e)))
					.filter(p -> p.getRight().getException() != null
							&& p.getRight().getThrowSite().getStartPosition() <= hoverRegion.getOffset()
							&& hoverRegion.getOffset() < p.getRight().getThrowSite().getStartPosition() + p.getRight().getThrowSite().getLength())
					.sorted(Comparator.comparingInt(p -> -p.getRight().getThrowSite().getStartPosition()))
					.collect(Collectors.toList());
		}

		if (infoPairs.isEmpty()) return null;
		return new GhostCommentInputCreator(getEditor(), infoPairs);
	}

	/*
	 * @see ITextHoverExtension#getHoverControlCreator()
	 * @since 3.0
	 */
	@Override
	public IInformationControlCreator getHoverControlCreator() {
		return parentShell -> new GhostCommentInformationControl(parentShell, getEditor());
	}
}