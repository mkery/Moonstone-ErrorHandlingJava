package recommend;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Created by florian on 11/14/16.
 */
public class RelevantHandler {
    private final String file;
    private final String type;
    private final String method;
    private final ASTNode node;

    public RelevantHandler(String file, String type, String method, ASTNode node) {
        this.file = file;
        this.type = type;
        this.method = method;
        this.node = node;
    }

    public String getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }

    public ASTNode getCode() {
        return node;
    }

}
