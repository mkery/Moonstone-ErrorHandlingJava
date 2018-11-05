package moonstone;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.quickfix.ResolutionGenerator;
import edu.cmu.moonstone.quickfix.ResolutionInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;

import java.util.*;

import static moonstone.Problem.*;

/** AST Walk that checks "try" and "catch" statements for poor practices
 * 
 * @author Mary Beth Kery, Steven Moore, Michael Puskas
 */
public class ExceptionVisitor extends ASTVisitor {
	private final boolean debug = true;
	private final ProblemsInErrorHandler problemsInErrorHandler;
	private final IProgressMonitor monitor;
	private final ResolutionGenerator resolutionGenerator = new ResolutionGenerator();

	private IResource currentResource; //current user's code file
	private CompilationUnit cu;

	private static final String[] ignoredWords = { "ignore", "ignored", "swallow" };

	private final Stack<MethodDeclaration> methodParent = new Stack<>();
	private final Stack<TryStatement> currentTryStatement = new Stack<>();
	private final Stack<List<ITypeBinding>> currentExceptionsThrownInTry = new Stack<>();
	private final Stack<CatchClause> currentCatchClause = new Stack<>();
	private final Stack<Block> currentFinally = new Stack<>();
	private final Stack<List<String>> suppressedList = new Stack<>();
	private final Stack<Map<IVariableBinding, List<MethodInvocation>>> currentClosures = new Stack<>();

	private IType autoClosableType;
	private IMethod closeMethod;
	private MethodOverrideTester overrideTester;

	public ExceptionVisitor(ProblemsInErrorHandler problemsInErrorHandler, IProgressMonitor monitor) {
		this.problemsInErrorHandler = problemsInErrorHandler;
		this.monitor = monitor;
	}

	@Override
	public void preVisit(ASTNode node) {
		if (monitor.isCanceled()) throw new OperationCanceledException();
	}

	private void pushMethodParent(MethodDeclaration methodParent) {
		//Get the list of suppressed warnings from the surrounding method
		this.methodParent.push(methodParent);
		pushTry(null);
		suppressedList.push(getSuppressedList(methodParent));
		currentClosures.add(new HashMap<>());
	}

	private void popMethodParent() {
		methodParent.pop();
		popTry();
		suppressedList.pop();
		currentClosures.pop();
	}

	private void pushTry(TryStatement node) {
		currentTryStatement.push(node);
		currentExceptionsThrownInTry.push(EditorUtility.getExceptionsThrownIn(node));
		currentCatchClause.push(null);
		currentFinally.push(null);
	}

	private void popTry() {
		currentTryStatement.pop();
		currentExceptionsThrownInTry.pop();
		currentCatchClause.pop();
		currentFinally.pop();
	}

	private <T> T get(Stack<T> stack) {
		return stack.isEmpty() ? null : stack.peek();
	}

	private List<String> getSuppressedList() {
		return get(suppressedList);
	}
	
	private MethodDeclaration getMethodParent() {
		return get(methodParent);
	}

	private TryStatement getTry() {
		return get(currentTryStatement);
	}

	private CatchClause getCatch() {
		return get(currentCatchClause);
	}

	private Block getFinally() {
		return get(currentFinally);
	}

	private List<ITypeBinding> getExceptionsThrownInTry() {
		return get(currentExceptionsThrownInTry);
	}

	private Map<IVariableBinding, List<MethodInvocation>> getClosures() {
		return get(currentClosures);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		pushMethodParent(node);

		if(!getSuppressedList().contains("badcatch")) //If we aren't suppressing this detection
		{
			List<ITypeBinding> exceptionsThrown = EditorUtility.getExceptionsThrownIn(node);
			List<Type> exceptionTypes = node.thrownExceptionTypes();
			for(Type t : exceptionTypes)
				verifyThrowsExceptionType(t, new Region(t.getStartPosition(), t.getLength()), exceptionsThrown, node, t);
		}

		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		if(!getSuppressedList().contains("resourceleak"))
			reportUnclosedResources();
		popMethodParent();
	}

	@Override
	public boolean visit(TryStatement node)
	{
		if(debug) System.out.println("Visiting a try block!");
		pushTry(node);
		return true; //allow for nested try blocks
	}

	@Override
	public void endVisit(TryStatement node) {
		currentTryStatement.pop();
	}

	@Override
	public boolean visit(Block node) {
		TryStatement currentTry = getTry();
		if (currentTry != null && currentTry.getFinally() == node)
			currentFinally.push(node);
		return true;
	}

	@Override
	public void endVisit(Block node) {
		if (getFinally() == node)
			currentFinally.pop();
	}

	@Override
	public boolean visit(CatchClause c) {
		currentCatchClause.push(c);

		if(!getSuppressedList().contains("badcatch")) //If we aren't suppressing this detection
		{
			TryStatement parent = (TryStatement) c.getParent();

			/* For each exception type caught, verify that the type is appropriate */
			SingleVariableDeclaration decl = c.getException();
			Type declType = decl.getType();
			List<ITypeBinding> exceptionsThrownInTry = getExceptionsThrownInTry();
			if(declType.isUnionType()) //eg. catch(ExceptionA | IOException | CoreException)
			{
				UnionType ut = (UnionType) declType;
				for(Type t : (Iterable<Type>) ut.types())
					verifyCatchExceptionType(t, new Region(t.getStartPosition(), t.getLength()), exceptionsThrownInTry, parent, c);
			}
			else //eg. catch(ExceptionA)
			{
				// Mark entire declaration as error, if verification fails
				verifyCatchExceptionType(declType, new Region(decl.getStartPosition(), decl.getLength()), exceptionsThrownInTry, parent, c);
			}

			if(debug) System.out.println("catching "+decl);
		}

		Block body = c.getBody();
		/* If the block is empty and we aren't suppressing this kind of detection */
		if(!getSuppressedList().contains("emptycatch") && body.statements().size() == 0)
		{
			boolean ignored = false;

			String identifier = c.getException().getName().getIdentifier();
			for (String ignoredWord : ignoredWords) {
				if (identifier.equalsIgnoreCase(ignoredWord)) {
					ignored = true;
					break;
				}
			}

			//TODO: This hasn't been tested very thoroughly, check it out if empty catches are acting funky
			if (!ignored)
				problemsInErrorHandler.report(EMPTY_CATCH, new Region(body.getStartPosition(), body.getLength()), currentResource);
		}

		return true;
	}

	@Override
	public void endVisit(CatchClause node) {
		currentCatchClause.pop();
	}

	@Override
	public boolean visit(ThrowStatement throwStatement) {
		if (verifyThrowInCatch(throwStatement)) return true;
		if (verifyThrow(throwStatement)) return true;
		return true;
	}

	private boolean verifyThrow(ThrowStatement throwStatement) {
		/* If it's a throw - Ex: throw new Exception - and, if we aren't suppressing this detection*/
		if(getSuppressedList().contains("badthrowfromtry")) return true;
		
		//Turn the Exception being thrown into a string
		ITypeBinding throwType = throwStatement.getExpression().resolveTypeBinding();
		//Check if it's improper and handle it if so
		verifyThrowFromTryExceptionType(throwType,
                new Region(throwStatement.getStartPosition(), throwStatement.getLength()));

		return true;
	}

	private boolean verifyThrowInCatch(ThrowStatement throwStatement) {
		CatchClause currentCatch = getCatch();
		if (currentCatch == null) return false;

		ITypeBinding typeBinding = throwStatement.getExpression().resolveTypeBinding();
		if (typeBinding == null) return false;

		if(!getSuppressedList().contains("badthrowfromcatch")) //If we aren't suppressing this detection
        {
            /* Check if it's throwing a bad exception */
			verifyThrowFromCatchExceptionType(typeBinding, new Region(throwStatement.getStartPosition(), throwStatement.getLength()));
		}

		if(!getSuppressedList().contains("destructivewrap")) //If we aren't suppressing this detection
        {
			SingleVariableDeclaration exception = currentCatch.getException();
			IBinding caughtException = exception.getName().resolveBinding();
			if (debug) System.out.println("[destructivewrap]: Looking for " + caughtException.toString());

			final boolean[] foundVar = {false};
			final ClassInstanceCreation[] instanceCreation = {null};

			throwStatement.accept(new ASTVisitor() {
				private ITypeBinding throwable = throwStatement.getAST().resolveWellKnownType(Throwable.class.getName());

				@Override
				public boolean preVisit2(ASTNode node) {
					return !foundVar[0];
				}

				@Override
				public boolean visit(SimpleName node) {
					foundVar[0] |= caughtException.isEqualTo(node.resolveBinding());
					return true;
				}

				@Override
				public boolean visit(ClassInstanceCreation node) {
					if (instanceCreation[0] != null) return true;
					IMethodBinding constructorBinding = node.resolveConstructorBinding();
					if (constructorBinding == null) return true;
					ITypeBinding returnType = constructorBinding.getDeclaringClass();
					if (returnType.isAssignmentCompatible(throwable)) {
						instanceCreation[0] = node;
					}
					return true;
				}
			});

			if (!foundVar[0] && instanceCreation[0] != null) {
				problemsInErrorHandler.report(DESTRUCTIVE_WRAP, new Region(instanceCreation[0].getStartPosition()
						, instanceCreation[0].getLength()), currentResource); //Report it

			}
        }

		return true;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if (getMethodParent() == null) return true;
		TryStatement tryStatement = getTry();
		if (tryStatement != null && tryStatement.resources().contains(node.getParent())) return true;

		IVariableBinding binding = node.resolveBinding();
		if (binding == null) return true;
		IType type = (IType) binding.getType().getJavaElement();
		try {
			if (type != null && autoClosableType != null && SuperTypeHierarchyCache.getTypeHierarchy(type).contains(autoClosableType))
                getClosures().put(binding, null);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		// Only record closures in finally
		if (getFinally() == null) return true;

		if (!(node.getExpression() instanceof SimpleName)) return true;
		SimpleName variableName = (SimpleName) node.getExpression();

		IBinding binding = variableName.resolveBinding();
		if (!(binding instanceof IVariableBinding)) return true;
		IVariableBinding variableBinding = (IVariableBinding) binding;

		IMethodBinding methodBinding = node.resolveMethodBinding();
		if (methodBinding == null) return true;

		IMethod method = (IMethod) methodBinding.getJavaElement();
		try {
			if (overrideTester != null && closeMethod != null
					&& overrideTester.isSubsignature(method, closeMethod)) {
				getClosures().compute(variableBinding, (var, mi) -> {
					if (mi == null) mi = new ArrayList<>();
					mi.add(node);
					return mi;
				});
			}
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	private void reportUnclosedResources() {
		Iterable<IVariableBinding> variableBindings = getClosures().entrySet().stream()
				.filter(entry -> entry.getValue() == null).map(Map.Entry::getKey)::iterator;
		for (IVariableBinding variableBinding : variableBindings) {
			ASTNode node = cu.findDeclaringNode(variableBinding);
			Region pos = new Region(node.getStartPosition(), node.getLength());
			problemsInErrorHandler.report(RESOURCE_LEAK, pos, currentResource);
		}
	}

	public static Problem getCommonExceptionProblem(ITypeBinding typeBinding) {
		switch(typeBinding.getQualifiedName())
		{
			case "java.lang.Exception":
				return CATCH_EXCEPTION;
			case "java.lang.Throwable":
				return CATCH_THROWABLE;
			case "java.lang.RuntimeException":
				return CATCH_RUNTIMEEXCEPTION;
			case "java.lang.Error":
				return CATCH_ERROR;
		}
		return NONE;
	}

	private void verifyCatchExceptionType(Type t, Region pos, List<ITypeBinding> exceptionsThrown, ASTNode parent, ASTNode node)
	{
		ITypeBinding typeBinding = t.resolveBinding();
		if (typeBinding == null) //Should only enter if type matches an existing type
		{
			if (debug) System.out.println("Catch Exception Type Binding NULL");
			return;
		}
		if (debug) System.out.println("Catch Exception Type Binding " + typeBinding.getQualifiedName());

		Position position = new Position(pos.getOffset(), pos.getLength());
		List<ResolutionInfo> resolutions = resolutionGenerator.getResolutions((IFile) currentResource,
				MarkerDef.CAUGHT_EXCEPTION_MARKER, exceptionsThrown, parent, node, position);
		if (resolutions.isEmpty()) return;

		//Remove any old bad catch marker
		Problem problem = getCommonExceptionProblem(typeBinding);
		problemsInErrorHandler.report(problem, pos, currentResource);
	}

	private void verifyThrowsExceptionType(Type t, Region pos, List<ITypeBinding> exceptionsThrown, ASTNode parent, ASTNode node)
	{
		ITypeBinding typeBinding = t.resolveBinding();
		if (typeBinding == null) //Should only enter if type matches an existing type
		{
			if (debug) System.out.println("Throws Exception Type Binding NULL");
			return;
		}
		if (debug) System.out.println("Throws Exception Type Binding " + typeBinding.getQualifiedName());

		Position position = new Position(pos.getOffset(), pos.getLength());
		List<ResolutionInfo> resolutions = resolutionGenerator.getResolutions((IFile) currentResource,
				MarkerDef.THROWS_EXCEPTION_MARKER, exceptionsThrown, parent, node, position);
		if (resolutions.isEmpty()) return;

		Problem problem = NONE;
		switch(typeBinding.getQualifiedName())
		{
			case "java.lang.Exception":
				problem = THROWS_EXCEPTION;
				break;
			case "java.lang.Throwable":
				problem = THROWS_THROWABLE;
		}
		problemsInErrorHandler.report(problem, pos, currentResource);
	}

	/** Examine the given string. If it's a bad exception to throw, report it.
	 *  @param type The type of the exception to be checked
	 * @param pos The position of the string
	 */
	private void verifyThrowFromTryExceptionType(ITypeBinding type, Region pos)
	{
		if (type == null) return;
		switch(type.getQualifiedName())
		{
		case "java.lang.Exception":
			problemsInErrorHandler.report(THROWFROMTRY_EXCEPTION, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Exception!");
			break;
		case "java.lang.Throwable":
			problemsInErrorHandler.report(THROWFROMTRY_THROWABLE, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Throwable!");
			break;
		case "java.lang.RuntimeException":
			problemsInErrorHandler.report(THROWFROMTRY_RUNTIMEEXCEPTION, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing RuntimeException!");
			break;
		case "java.lang.Error":
			problemsInErrorHandler.report(THROWFROMTRY_ERROR, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Error!");
			break;
		}
	}
	
	/** Examine the given string. If it's a bad exception to throw, report it.
	 * 
	 * @param type The type of the exception to be checked
	 * @param pos The position of the string
	 */
	private void verifyThrowFromCatchExceptionType(ITypeBinding type, Region pos)
	{
		if (type == null) return;
		switch(type.getQualifiedName())
		{
		case "java.lang.Exception":
			problemsInErrorHandler.report(THROWFROMCATCH_EXCEPTION, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Exception!");
			break;
		case "java.lang.Throwable":
			problemsInErrorHandler.report(THROWFROMCATCH_THROWABLE, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Throwable!");
			break;
		case "java.lang.RuntimeException":
			problemsInErrorHandler.report(THROWFROMCATCH_RUNTIMEEXCEPTION, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing RuntimeException!");
			break;
		case "java.lang.Error":
			problemsInErrorHandler.report(THROWFROMCATCH_ERROR, pos, currentResource);
			if(debug) System.out.println("[walk]  throwing Error!");
			break;
		}
	}
	
	/** Given a MethodDeclaration, finds and returns any expressions from a @SuppressWarnings annotation, if it exists
	 * 
	 * @param methodParent The given MethodDeclaration
	 * @return A list of Expressions filled with the types of warnings to be suppressed, or an empty list if there is no @SuppressWarnings
	 */
	private List<String> getSuppressedList(MethodDeclaration methodParent)
	{
		/* Check if there is an annotation on the method */
		SingleMemberAnnotation existingAnnotation = null;
		List<IExtendedModifier> modList = methodParent.modifiers();

		for(IExtendedModifier mod : modList) //For each modifier in the list
			if(mod instanceof SingleMemberAnnotation) //If we found our annotation
				existingAnnotation = (SingleMemberAnnotation) mod; //Save it to existingAnnotation

		if(existingAnnotation == null) //If there is no existing annotation, return an empty list
			return new ArrayList<>();
		else //Else there is an existing annotation, return the list of the expressions from it
		{
			AST ast = methodParent.getAST();

			//Create an array and add the values to it
			ArrayInitializer valueArr = ast.newArrayInitializer();
			StringLiteral existingLiteral = null;
			
			if(existingAnnotation.getValue() instanceof StringLiteral) //If the existing annotation is a StringLiteral (single token)
			{
				existingLiteral = ast.newStringLiteral();
				existingLiteral.setLiteralValue(((StringLiteral) existingAnnotation.getValue()).getLiteralValue());
				valueArr.expressions().add(existingLiteral);
			}
			else //Else the existing annotation is an array initializer, add each separate token to the new array initializer
			{
				ArrayInitializer existingArray = (ArrayInitializer) existingAnnotation.getValue();
				List<Expression> expressionList = existingArray.expressions();
				
				for(Expression existingExpression : expressionList)
				{
					if(existingExpression instanceof StringLiteral)
					{
						existingLiteral = ast.newStringLiteral();
						existingLiteral.setLiteralValue(((StringLiteral) existingExpression).getLiteralValue());
						valueArr.expressions().add(existingLiteral);
					}
				}
			}
			
			//Convert the expression list to a list of strings for easier comparisons later
			List<Expression> expressionList = valueArr.expressions();
			List<String> suppressedList = new ArrayList<>();
			for(Expression exp : expressionList) //For each expression, chop off the quotation marks and add it to the list
			{
				String sExpression = exp.toString();
				sExpression = sExpression.substring(1, sExpression.length() - 1);
				suppressedList.add(sExpression);
			}

			return suppressedList;
		}
	}

	/**
	 * Starts the process.
	 *
	 * @param unit
	 *            the AST root node. Bindings have to have been resolved.
	 */
	public void process(IResource resource, CompilationUnit unit) {
		if(debug) System.out.println("[walk] Walking AST start");
		currentResource = resource;
		cu = (CompilationUnit) unit.getRoot();

		try {
			autoClosableType = cu.getJavaElement().getJavaProject().findType(AutoCloseable.class.getName());
			if (autoClosableType != null) {
				closeMethod = autoClosableType.getMethod("close", null);
				overrideTester = SuperTypeHierarchyCache.getMethodOverrideTester(autoClosableType);
			}
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}

		unit.accept(this);
	}

}
