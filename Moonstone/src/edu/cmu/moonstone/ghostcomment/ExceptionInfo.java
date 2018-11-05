package edu.cmu.moonstone.ghostcomment;

import moonstone.ASTStatementVisitor;
import moonstone.EditorUtility;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Ties an exception name to a line number where it is caught
 * 
 * @author Michael Puskas
 */
public class ExceptionInfo 
{
	private final ASTNode throwSite; //The node to attach the Ghost Comment to
	private final ASTNode call;
	private final int originLineNumber;

	private final ITypeBinding exception; //The fully qualified name of the exception thrown
	private final CaughtStep caughtStep;
	private final List<FlowStep> flowSteps = new ArrayList<>();
	private final List<ExceptionInfo> subExceptions = new ArrayList<>();

	public ExceptionInfo(IDocument doc, ASTNode throwSite, ASTNode call, ITypeBinding exception)
	{
		this.throwSite = throwSite;
		this.call = call;
		int originLineNumber;
		try {
			originLineNumber = doc.getLineOfOffset(throwSite.getStartPosition() + throwSite.getLength() - 1);
		} catch (BadLocationException e) {
			originLineNumber = -1;
		}
		this.originLineNumber = originLineNumber;

		this.exception = exception;
		// Determine catch handler
		caughtStep = determineFlow(doc);
	}
	
	public ExceptionInfo(IDocument doc, TryStatement tryStatement)
	{
		this.throwSite = tryStatement;
		this.call = tryStatement;
		int originLineNumber;
		try {
			originLineNumber = doc.getLineOfOffset(tryStatement.getStartPosition());
		} catch (BadLocationException e) {
			originLineNumber = -1;
		}
		this.originLineNumber = originLineNumber;

		this.exception = null;
		// Determine catch handler
		caughtStep = null;
		determineFlow(doc, tryStatement);
	}

	private CaughtStep determineFlow(IDocument doc) {
		CaughtStep[] caughtStep = new CaughtStep[] {null};

		/*
		 * Run through the classes being caught, find the CatchClause that catches the currentException
		 * When found, save the line number
		 */
		EditorUtility.acceptParents(throwSite, new ASTStatementVisitor() {
			@Override
			public boolean visit(TryStatement tryParent) {
				Block tryBody = tryParent.getBody();
				int tryBodyStart = tryBody.getStartPosition();
				int nodeStart = throwSite.getStartPosition();
				// Is try statement applicable?
				if (tryBodyStart > nodeStart
						|| tryBodyStart + tryBody.getLength() < nodeStart + throwSite.getLength()) {
					addFinallyToFlow(doc, tryParent);
					return true;
				}

				List<CatchClause> catchClauses = tryParent.catchClauses();
				outer:
				for (CatchClause catchClause : catchClauses) {
					//Fill typeList with the caught exceptions
					Stream<ITypeBinding> caughtTypes;
					Type type = catchClause.getException().getType();
					if (type.isUnionType()) //eg. catch(ExceptionA | IOException | CoreException)
					{
						UnionType unionType = (UnionType) type;
						List<Type> types = unionType.types();
						caughtTypes = types.stream().map(Type::resolveBinding);
					} else //eg. catch(ExceptionA)
					{
						caughtTypes = Stream.of(type.resolveBinding());
					}

					//Check if the currentException is a child of any of the caught classes
					Iterable<ITypeBinding> caughtExceptions = caughtTypes.filter(Objects::nonNull)::iterator;
					for (ITypeBinding caughtClass : caughtExceptions) { //For each of the caught classes in this catch block
						if (exception.isAssignmentCompatible(caughtClass)) //If the caught class is, or is a parent of, the current exception
						{
							flowSteps.add(caughtStep[0] = new CaughtStep(doc, catchClause));
							break outer;
						} else if (caughtClass.isAssignmentCompatible(exception)) {
							getSubExceptions().add(new ExceptionInfo(doc, throwSite, call, caughtClass));
						}
					}
				}

				addFinallyToFlow(doc, tryParent);
				return caughtStep[0] == null;
			}
		});

		if (caughtStep[0] != null)
			return caughtStep[0];

		PropagateStep propagateStep = createPropagateStep(doc, EditorUtility.inMethodDeclaration(throwSite));
		flowSteps.add(propagateStep);
		return propagateStep;
	}
	
	private void determineFlow(IDocument doc, TryStatement tryParent) {
		List<Statement> statements = tryParent.getBody().statements();
		statements.stream().findFirst().ifPresent(statement -> {
			try {
				int lineNumber = doc.getLineOfOffset(statement.getStartPosition());
				flowSteps.add(new InfoStep(String.format("Statements in try body are executed starting from line %d", lineNumber + 1), lineNumber));
			} catch (BadLocationException e) {
				flowSteps.add(new InfoStep("Statements in try body are executed"));
			}

		});
		List<VariableDeclarationExpression> resources = tryParent.resources();
		resources.stream()
				.<VariableDeclarationFragment>flatMap(decl -> decl.fragments().stream())
				.forEach(resource -> flowSteps.add(new ResourceCleanupStep(doc, tryParent, resource)));
		if (!tryParent.catchClauses().isEmpty())
			flowSteps.add(new InfoStep("catch handler is executed in case of matching exception"));
		addFinallyToFlow(doc, tryParent);
	}

	private void addFinallyToFlow(IDocument doc, TryStatement tryParent) {
		Block tryFinally = tryParent.getFinally();
		if (tryFinally == null) return;

		List<CatchClause> catchClauses = tryParent.catchClauses();
		ASTNode prevNode = catchClauses.isEmpty()
                ? tryParent.getBody()
                : catchClauses.get(catchClauses.size() - 1);
		flowSteps.add(new FinallyStep(doc, tryFinally, prevNode));
	}

	public ASTNode getThrowSite()
	{
		return throwSite;
	}

	public ASTNode getCall() {
		return call;
	}

	public ITypeBinding getException() 
	{
		return exception;
	}

	public int getOriginLineNumber()
	{
		return originLineNumber;
	}

	public CaughtStep getCaughtStep() {
		return caughtStep;
	}

	public List<FlowStep> getFlowSteps() {
		return flowSteps;
	}

	public List<ExceptionInfo> getSubExceptions() {
		return subExceptions;
	}

	public interface FlowStep {
		int getLineNumber();
		Position getBlock();
		Position getHighlight();
	}
	
	public class ResourceCleanupStep implements FlowStep {
		private final Position block;
		private final int lineNumber;
		private VariableDeclarationFragment resource;

		ResourceCleanupStep(IDocument doc, TryStatement tryStatement, VariableDeclarationFragment resource) {
			block = new Position(tryStatement.getStartPosition(), tryStatement.getLength());
			this.resource = resource;

			int lineNumber;
			try {
				lineNumber = doc.getLineOfOffset(resource.getStartPosition());
			} catch (BadLocationException e) {
				lineNumber = -1;
			}
			this.lineNumber = lineNumber;
		}

		@Override
		public Position getBlock() {
			return block;
		}

		@Override
		public Position getHighlight() {
			return new Position(resource.getStartPosition(), resource.getLength());
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		@Override
		public String toString() {
			SimpleName resourceName = resource.getName();
			return String.format("If %s was initialized non-null on line %d, %s.close() is always called", resourceName, lineNumber + 1, resourceName);
		}
	}

	public class InfoStep implements FlowStep {
		private final String message;
		private final int lineNumber;

		public InfoStep(String message) {
			this(message, -1);
		}

		public InfoStep(String message, int lineNumber) {
			this.message = message;
			this.lineNumber = lineNumber;
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		@Override
		public Position getBlock() {
			return null;
		}

		@Override
		public Position getHighlight() {
			return null;
		}

		@Override
		public String toString() {
			return message;
		}
	}

	public class CaughtStep implements FlowStep {
		private final Position block;
		private final Position highlight;
		private final int lineNumber;
		private ITypeBinding caughtType;

		CaughtStep(Position block, Position highlight, int lineNumber) {
			this.block = block;
			this.highlight = highlight;
			this.lineNumber = lineNumber;
		}

		CaughtStep(IDocument doc, CatchClause catchClause) {
			block = new Position(catchClause.getStartPosition(), catchClause.getLength());
			SingleVariableDeclaration exception = catchClause.getException();
			caughtType = exception.getType().resolveBinding();
			highlight = new Position(exception.getStartPosition(), exception.getLength());

			int lineNumber;
			try {
				lineNumber = doc.getLineOfOffset(catchClause.getStartPosition());
			} catch (BadLocationException e) {
				lineNumber = -1;
			}
			this.lineNumber = lineNumber;
		}

		@Override
		public Position getBlock() {
			return block;
		}

		@Override
		public Position getHighlight() {
			return highlight;
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		private String getCaughtName() {
			return caughtType != null && !caughtType.isEqualTo(exception)
					? String.format(" as supertype %s", caughtType.getName())
					: "";
		}

		@Override
		public String toString() {
			return String.format("%s is caught%s on line %d", exception.getName(), getCaughtName(), lineNumber + 1);
		}
	}

	public class PropagateStep extends CaughtStep {
		private final MethodDeclaration method;

		PropagateStep(MethodDeclaration method, Position block, Position highlight, int lineNumber) {
			super(block, highlight, lineNumber);
			this.method = method;
		}

		@Override
		public String toString() {
			return String.format("%s is thrown to caller of %s() as declared on line %d", exception.getName(), method.getName(), super.lineNumber + 1);
		}
	}

	private PropagateStep createPropagateStep(IDocument doc, MethodDeclaration method) {
		Position block = new Position(method.getStartPosition() + method.getLength());
		Position highlight = createPropagateHighlight(method);

		int lineNumber;
		try {
			lineNumber = doc.getLineOfOffset(highlight.getOffset());
		} catch (BadLocationException e) {
			lineNumber = -1;
		}
		return new PropagateStep(method, block, highlight, lineNumber);
	}

	private Position createPropagateHighlight(MethodDeclaration method) {
		List<Type> thrownExceptionTypes = method.thrownExceptionTypes();

		Optional<Type> thrownExceptionType = thrownExceptionTypes.stream()
				.filter(type -> exception.isAssignmentCompatible(type.resolveBinding()))
				.findFirst();
		if (thrownExceptionType.isPresent())
			return new Position(thrownExceptionType.get().getStartPosition(), thrownExceptionType.get().getLength());

		SimpleName methodName = method.getName();
		return new Position(methodName.getStartPosition(), methodName.getLength());
	}

	public class FinallyStep implements FlowStep {
		private static final String FINALLY = "finally";
		private final Position block;
		private final Position highlight;
		private final int lineNumber;

		FinallyStep(IDocument doc, Block tryFinally, ASTNode prevNode) {
			int finallyStart = prevNode.getStartPosition() + prevNode.getLength();
			int finallyEnd = tryFinally.getStartPosition() + tryFinally.getLength();
			try {
				while (finallyStart < finallyEnd && Character.isWhitespace(doc.getChar(finallyStart))) finallyStart++;
			} catch (BadLocationException ignored) {}
			block = new Position(finallyStart, finallyEnd - finallyStart);

			int finallyKeywordStart = finallyStart;
			try {
				finallyKeywordStart += doc.get(finallyStart, tryFinally.getStartPosition() - finallyStart)
						.indexOf(FINALLY);
			} catch (BadLocationException ignored) {}
			highlight = new Position(finallyKeywordStart, FINALLY.length());

			int lineNumber;
			try {
				lineNumber = doc.getLineOfOffset(finallyKeywordStart);
			} catch (BadLocationException e) {
				lineNumber = -1;
			}
			this.lineNumber = lineNumber;
		}

		@Override
		public int getLineNumber() {
			return lineNumber;
		}

		@Override
		public Position getBlock() {
			return block;
		}

		@Override
		public Position getHighlight() {
			return highlight;
		}

		@Override
		public String toString() {
			return FINALLY + " block is always executed on line " + (lineNumber + 1);
		}
	}
}
