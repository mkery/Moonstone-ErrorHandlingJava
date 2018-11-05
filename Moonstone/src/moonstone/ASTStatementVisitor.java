package moonstone;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Created by florian on 11/18/16.
 */
public class ASTStatementVisitor extends ASTVisitor {
    @Override
    public boolean visit(MethodDeclaration node) {
        return false;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        return false;
    }
}
