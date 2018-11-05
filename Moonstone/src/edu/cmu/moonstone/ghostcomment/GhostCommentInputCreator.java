package edu.cmu.moonstone.ghostcomment;

import moonstone.ParentDelegate;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.ui.IEditorPart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by florian on 4/7/17.
 */
public class GhostCommentInputCreator {
    private final IEditorPart editor;
    private final List<Pair<CommentInfo, ExceptionInfo>> infoPairs = new ArrayList<>();

    public GhostCommentInputCreator(IEditorPart editor, List<Pair<CommentInfo, ExceptionInfo>> infoPairs) {
        this.editor = editor;

        for (Pair<CommentInfo, ExceptionInfo> infoPair : infoPairs) {
            CommentInfo commentInfo = infoPair.getLeft();
            List<ExceptionInfo> queue = new ArrayList<>();
            queue.add(infoPair.getRight());
            while (!queue.isEmpty()) {
                ExceptionInfo exceptionInfo = queue.remove(queue.size() - 1);
                List<ExceptionInfo> subExceptions = exceptionInfo.getSubExceptions();
                for (int i = subExceptions.size() - 1; i >= 0; i--)
                    queue.add(subExceptions.get(i));

                this.infoPairs.add(Pair.of(commentInfo, exceptionInfo));
            }
        }
    }

    public Stream<Function<ParentDelegate, GhostCommentControl>> createControls(
            GhostCommentControl.EditorDelegate editorDelegate) {
        return infoPairs.stream().map(infoPair -> createCreator(infoPair, editorDelegate));
    }

    private Function<ParentDelegate, GhostCommentControl> createCreator(
            Pair<CommentInfo, ExceptionInfo> infoPair, GhostCommentControl.EditorDelegate editorDelegate) {
        return parentDelegate -> new GhostCommentControl(editor,
                infoPair.getLeft(), infoPair.getRight(), parentDelegate, editorDelegate);
    }
}
