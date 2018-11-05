package recommend;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;

/**
 * Created by florian on 11/14/16.
 */
public interface ExceptionHandler {
    ASTNode getNode();
    List<String> getStatements();
}
