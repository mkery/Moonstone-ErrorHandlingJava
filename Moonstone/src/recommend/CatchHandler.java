package recommend;

import org.eclipse.jdt.core.dom.*;
import recommend.experimentOnly.StatementWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by florian on 11/14/16.
 */
public class CatchHandler implements ExceptionHandler {
    private final CatchClause node;
    private final List<String> exceptionTypes;
    private final List<String> statements;

    /**
     * Given a catch clause and a list of features, adds each exception type caught in c
     * to the list of features. Takes into account union types as well as normal singleton types.
     * */
    public static List<String> getExceptionTypes(CatchClause c)
    {
        //record type of exception caught
        Type ext = c.getException().getType();

        //if union type of form catch(IOException | CoreException e), get each exception
        if(ext instanceof UnionType)
        {
            UnionType unionType = (UnionType) ext;
            List<Type> types = unionType.types();
            return types.stream()
                    .map(Type::resolveBinding)
                    .filter(Objects::nonNull)
                    .map(ITypeBinding::getName)
                    .collect(Collectors.toList());
        }
        else //if just a singleton type, add directly as a feature
        {
            List<String> exceptionTypes = new ArrayList<>(1);

            ITypeBinding binding = ext.resolveBinding();
            if (binding != null) //sadly not always possible to resolve type statically
                exceptionTypes.add(binding.getQualifiedName());
            else
            {
                //if(debug) System.out.println("[recParser] unable to resolve binding of exception type: "+ext.toString());
                exceptionTypes.add(ext.toString());
            }

            return exceptionTypes;
        }
    }

    public CatchHandler(CatchClause catchClause) {
        node = catchClause;

        //add to it all exception types caught by this clause
        exceptionTypes = getExceptionTypes(catchClause);

        //now canonicalize each statement in the catch body
        Block body = catchClause.getBody();

        //a list of canonicalized statements in the catch block
        statements = StatementWalker.extractStatements(body);
    }

    @Override
    public CatchClause getNode() {
        return node;
    }

    @Override
    public List<String> getStatements() {
        return statements;
    }

    public List<String> getExceptionTypes() {
        return exceptionTypes;
    }
}
