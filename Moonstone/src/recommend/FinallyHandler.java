package recommend;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.TryStatement;
import recommend.experimentOnly.StatementWalker;

import java.util.List;

/**
 * Created by florian on 11/14/16.
 */
public class FinallyHandler implements ExceptionHandler {
    private final Block node;
    private final List<String> statements;

    public FinallyHandler(TryStatement tryStatement) {
        node = tryStatement.getFinally();

        //a list of canonicalized statements in the finally block
        statements = StatementWalker.extractStatements(node);
    }

    @Override
    public Block getNode() {
        return node;
    }

    @Override
    public List<String> getStatements() {
        return statements;
    }
}
