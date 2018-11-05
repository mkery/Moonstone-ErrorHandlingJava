package moonstone;

import edu.cmu.moonstone.ghostcomment.CommentInfo;
import edu.cmu.moonstone.ghostcomment.ExceptionInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * [mb] Helper methods for different functions that effect the user's code in an editor.
 */

public class EditorUtility {


	/***
	 * [mb] parses compilation unit into AST, focused on a cursor position
	 * @param unit - the compilation unit that contains this AST
	 * @param position - the offset position of the point we're selecting in this program's document
	 * @return an AST abridged to only include pieces relevant to the given selection
	 */
	public static ASTNode parseASTFocused(ICompilationUnit unit, int position) {
		try {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			parser.setIgnoreMethodBodies(false);
			parser.setFocalPosition(position);//only the abridged ast, the nodes that contain this position
			return parser.createAST(null); // parse
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
    }
	
	/*
	 * [mb] parses compilation unit into AST
	 */
	public static CompilationUnit parseAST(ICompilationUnit unit) {
		try {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			parser.setIgnoreMethodBodies(false);
			return (CompilationUnit) parser.createAST(null); // parse
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
    }

	/** If the given node is in a TryStatement, return the TryStatement parent
	 * 
	 * @param node The node to check
	 * @return The parent TryStatement if it exists, otherwise null
	 */
	public static TryStatement inTryStatement(ASTNode node)
	{
		if (node == null) return null;
		ASTNode astNode = node.getNodeType() == ASTNode.TRY_STATEMENT
				? node
				: ASTNodes.getParent(node, ASTNode.TRY_STATEMENT);
		return (TryStatement) astNode;
	}

	public static CatchClause inCatchClause(ASTNode node)
	{
		if (node == null) return null;
		ASTNode astNode = node.getNodeType() == ASTNode.CATCH_CLAUSE
				? node
				: ASTNodes.getParent(node, ASTNode.CATCH_CLAUSE);
		return (CatchClause) astNode;
	}

	/** If the given node is in a MethodDeclaration, return the MethodDeclaration parent
	 *
	 * @param node The node to check
	 * @return The parent MethodDeclaration if it exists, otherwise null
	 */
	public static MethodDeclaration inMethodDeclaration(ASTNode node)
	{
		if (node == null) return null;
		ASTNode astNode = node.getNodeType() == ASTNode.METHOD_DECLARATION
				? node
				: ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
		return (MethodDeclaration) astNode;
	}

	private static boolean visit(ASTNode node, ASTVisitor visitor) {
		switch(node.getNodeType()) {
			case ASTNode.BLOCK:
				return visitor.visit((Block) node);
			case ASTNode.CATCH_CLAUSE:
				return visitor.visit((CatchClause) node);
			case ASTNode.METHOD_DECLARATION:
				return visitor.visit((MethodDeclaration) node);
			case ASTNode.THROW_STATEMENT:
				return visitor.visit((ThrowStatement) node);
			case ASTNode.TRY_STATEMENT:
				return visitor.visit((TryStatement) node);
			case ASTNode.TYPE_DECLARATION:
				return visitor.visit((TypeDeclaration) node);
			case ASTNode.METHOD_INVOCATION:
				return visitor.visit((MethodInvocation) node);
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return visitor.visit((ClassInstanceCreation) node);
			default:
				return true;
		}
	}

	private static void endVisit(ASTNode node, ASTVisitor visitor) {
		switch(node.getNodeType()) {
			case ASTNode.BLOCK:
				visitor.endVisit((Block) node);
				break;
			case ASTNode.CATCH_CLAUSE:
				visitor.endVisit((CatchClause) node);
				break;
			case ASTNode.METHOD_DECLARATION:
				visitor.endVisit((MethodDeclaration) node);
				break;
			case ASTNode.THROW_STATEMENT:
				visitor.endVisit((ThrowStatement) node);
				break;
			case ASTNode.TRY_STATEMENT:
				visitor.endVisit((TryStatement) node);
				break;
			case ASTNode.TYPE_DECLARATION:
				visitor.endVisit((TypeDeclaration) node);
				break;
			case ASTNode.METHOD_INVOCATION:
				visitor.endVisit((MethodInvocation) node);
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION:
				visitor.endVisit((ClassInstanceCreation) node);
				break;
		}
	}

	/**
	 * Visits all the parents of the given node.
	 * WARNING: Does not support all node types. Does not visit unsupported ones.
	 * */
	public static void acceptParents(ASTNode node, ASTVisitor visitor) {
		if (node == null) return;
		if (visitor.preVisit2(node)) {
			try {
				if (visit(node, visitor)) {
					ASTNode parent = node.getParent();
					if (parent != null) acceptParents(parent, visitor);
				}
				endVisit(node, visitor);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
		visitor.postVisit(node);
	}
	private static Pattern tryLeftSnap = Pattern.compile("^\\s?try(?:\\s|[({])");

	public static TryStatement inTryStatement(IEditorPart editor, int offset) {
    	/* Check if we have entered/left the body of a try statement */
		IJavaElement inputJavaElement = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		if (!(inputJavaElement instanceof ICompilationUnit)) return null;

		ICompilationUnit cu = (ICompilationUnit) inputJavaElement;
		outer:
		try {
			IBuffer buffer = cu.getBuffer();
			int minOffset = Math.max(0, offset - 2), newOffset = offset;
			for (; minOffset <= newOffset; newOffset--) {
				char c = buffer.getChar(newOffset);
				if (c == '}') {
					offset = newOffset;
					break outer;
				} else if (!Character.isWhitespace(c)) break;
			}

			Matcher matcher = tryLeftSnap.matcher(buffer.getText(offset, 5));
			if (matcher.find()) offset += matcher.end();
		} catch (Throwable t) {
			System.err.println("Could not snap to try");
			t.printStackTrace();
		}

		//ASTNode oldNode = StartupUtility.getSelectedNode(cu, oldOffset);
		ASTNode newNode = getSelectedNode(cu, offset);
		//TryStatement oldParent = EditorUtility.inExceptionHandler(oldNode);
		return inTryStatement(newNode);
	}

	/*
	 * [mb] Given an arbitrary ASTNode n, if n is within a statement that contains a function call
	 * eg. foo = bar(foobar(n)) ... this method will return a String formatted list the exceptions of
	 * all functions in the same statement as n, here foobar and bar. If there are no functions OR
	 * no functions declare possible exceptions, "" is returned.
	 */
	public static List<Pair<ASTNode, List<ITypeBinding>>> checkForQualifiedExceptions(ASTNode n)
	{
		List<Pair<ASTNode, List<ITypeBinding>>> empty = new ArrayList<>(0);
		if(n == null) return empty;

		List<Pair<ASTNode, List<ITypeBinding>>> exceptionBindings = new ArrayList<>();

        /*
         * Next, examine everything in this line of code to see if there is anything that will throw
         * a checked Exception. DANGER: only examine nodes on this line
         */
        n.accept(new ASTStatementVisitor() {
        	private Stack<List<Pair<ASTNode, List<ITypeBinding>>>> currentExceptions;
			{
				currentExceptions = new Stack<>();
				currentExceptions.push(exceptionBindings);
			}

        	public boolean visit(MethodInvocation invok)
        	{
				addBindings(invok.resolveMethodBinding(), invok);
				return true;
        	}

			public boolean visit(ClassInstanceCreation create)
        	{
				addBindings(create.resolveConstructorBinding(), create);
				return true;
        	}

			@Override
			public boolean visit(ThrowStatement node) {
				ITypeBinding exp = node.getExpression().resolveTypeBinding();
				ArrayList<ITypeBinding> list = new ArrayList<>();
				list.add(exp);
				currentExceptions.peek().add(Pair.of(node, list));
				return true;
			}

			private void addBindings(IMethodBinding methodBinding, ASTNode node) {
				if (methodBinding == null) return;
				ITypeBinding[] exp = methodBinding.getExceptionTypes();
				if(exp != null && exp.length > 0) {
					ArrayList<ITypeBinding> list = new ArrayList<>();
					Collections.addAll(list, exp);
					currentExceptions.peek().add(Pair.of(node, list));
				}
			}

			@Override
			public boolean visit(Block node) {
				if (node != n && node.getLocationInParent() == TryStatement.BODY_PROPERTY)
					currentExceptions.push(new ArrayList<>());
				return true;
			}

			@Override
			public void endVisit(Block node) {
				if (node != n && node.getLocationInParent() == TryStatement.BODY_PROPERTY) {
					TryStatement tryParent = (TryStatement) node.getParent();
					List<CatchClause> catchClauses = tryParent.catchClauses();
					List<Pair<ASTNode, List<ITypeBinding>>> exceptions = currentExceptions.pop();
					catchClauses.stream()
							.map(CatchClause::getException)
							.map(SingleVariableDeclaration::getType)
							.flatMap(type -> type.isUnionType() ? ((List<Type>) ((UnionType) type).types()).stream() : Stream.of(type))
							.map(Type::resolveBinding)
							.forEach(caught -> exceptions.forEach(p -> p.getRight().removeIf(e -> e.isAssignmentCompatible(caught))));
					currentExceptions.peek().addAll(exceptions);
				}
			}
		});

		return exceptionBindings;
	}


	/** Returns a List<String> containing the exceptions that can potentially be thrown by the body of the given try statement
	 * 
	 * @author Michael Puskas
	 * @param tryState A try statement
	 * @return A list of the possible exceptions that can be thrown by the try body. Tostring = "[]" if empty
	 */
	public static List<ITypeBinding> getExceptionsThrownIn(TryStatement tryState)
	{
		if(tryState == null) return new ArrayList<>();
		return checkForQualifiedExceptions(tryState.getBody()).stream()
				.flatMap(pair -> pair.getRight().stream())
				.distinct()
				.collect(Collectors.toList());
	}
	
	public static List<ITypeBinding> getExceptionsThrownIn(MethodDeclaration method)
	{
		if(method == null) return new ArrayList<>();
		return checkForQualifiedExceptions(method.getBody()).stream()
				.flatMap(pair -> pair.getRight().stream())
				.distinct()
				.collect(Collectors.toList());
	}

	/** Returns the ASTNode given a cursor position
     *
     * @param cu The appropriate ICompilationUnit
     * @param cursorPosition The cursor position to return an ASTNode for
     * @return The ASTNode at the given cursor position
     */
    public static ASTNode getSelectedNode(ICompilationUnit cu, int cursorPosition) {
		return getSelectedNode(parseAST(cu), cursorPosition, 1);
	}

	public static ASTNode getSelectedNode(ICompilationUnit cu, int cursorPosition, int length) {
    	return getSelectedNode(parseAST(cu), cursorPosition, length);
	}

	public static ASTNode getSelectedNode(ASTNode root, int cursorPosition, int length) {
    	if (root == null) return null;
        NodeFinder nodeFinder = new NodeFinder(root, cursorPosition, length);
        ASTNode node = nodeFinder.getCoveredNode();
        return node != null ? node : nodeFinder.getCoveringNode();
    }

	public static Optional<int[]> getExceptionParameterMapping(SingleVariableDeclaration exception, ClassInstanceCreation constructor) {
		List<ITypeBinding> argumentBindings = Stream.concat(((List<Expression>) constructor.arguments()).stream(), Stream.of(exception.getName()))
				.map(Expression::resolveTypeBinding)
				.collect(Collectors.toList());
		IMethodBinding[] declaredMethods = constructor.resolveConstructorBinding().getDeclaringClass().getDeclaredMethods();
		return Arrays.stream(declaredMethods)
				.filter(IMethodBinding::isConstructor)
				.filter(mb -> mb.getParameterTypes().length >= argumentBindings.size())
				.map(mb -> {
					ArrayList<ITypeBinding> args = new ArrayList<>(argumentBindings);
					return Arrays.stream(mb.getParameterTypes()).mapToInt(par -> {
						for (int i = 0; i < args.size(); i++) {
							ITypeBinding arg = args.get(i);
							if (arg != null && arg.isAssignmentCompatible(par)) {
								args.set(i, null);
								return i;
							}
						}
						return -1;
					}).toArray();
				})
				.filter(mapping -> Arrays.stream(mapping).allMatch(m -> m >= 0))
				.findFirst();
	}

    private static class CommentParts {
		public String commentText;
		public TreeMap<Integer, ExceptionInfo> exceptionsAssociated = new TreeMap<>();
	}

	/** Returns a List<CommentInfo> containing the info for the exceptions that can potentially be thrown by the body of the given try statement
	 * 
	 * @author Michael Puskas
	 * @param gc
	 * @param tryParent A try statement
	 * @return A list of the possible exceptions that can be thrown by the try body. Tostring = "[]" if empty
	 */
	public static List<CommentInfo> getCommentInfoThrownByTryBody(IDocument doc, GC gc, TryStatement tryParent)
	{
		if (tryParent == null) return new ArrayList<>();
		List<Comment> commentList = ((CompilationUnit) tryParent.getRoot()).getCommentList();

		final Map<Integer, List<Comment>> commentsPerLine = commentList.stream().collect(
				Collectors.groupingBy(
						comment -> {
							try {
								return doc.getLineOfOffset(comment.getStartPosition());
							} catch (BadLocationException e) {
								throw new RuntimeException(e);
							}
						},
						Collectors.toList()
				)
		);
		final Map<Comment, CommentParts> commentParts = new HashMap<>();
		final List<CommentInfo> commentInfos = new ArrayList<>();
		final Map<Integer, List<ExceptionInfo>> ghostInfos = new TreeMap<>();

		tryParent.accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				int lineNo = getLineNo(doc, node.getBody().getStartPosition());
				if (lineNo == -1) return true;

				List<VariableDeclarationExpression> resources = node.resources();
				Stream<String> fragmentStream = resources.stream()
						.<List<VariableDeclarationFragment>>map(VariableDeclarationExpression::fragments)
						.flatMap(Collection::stream)
						.map(VariableDeclarationFragment::getName)
						.map(SimpleName::getIdentifier)
						.map(s -> String.format("%s.close()", s));

				String calls = String.join(", ", (Iterable<String>) fragmentStream::iterator);
				if (calls.isEmpty()) return true;

				int commentOffset = determineGhostCommentOffsetForLine(doc, lineNo);
				ExceptionInfo exceptionInfo = new ExceptionInfo(doc, node);
				commentInfos.add(CommentInfo.create(gc, exceptionInfo, commentOffset, String.format(" // try-with-resources automatically calls %s ", calls)));
				return true;
			}

			@Override
			public boolean visit(ThrowStatement node) {
				ITypeBinding exception = node.getExpression().resolveTypeBinding();
				if (exception == null) return true;
				try {
					String code = doc.get(node.getStartPosition(), node.getLength());
					ExceptionInfo exceptionInfo = new ExceptionInfo(doc, node, node.getExpression(), exception);
					commentInfos.add(CommentInfo.create(gc, Collections.singletonList(exceptionInfo), node.getStartPosition(), Arrays.asList("", code)));
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
				return true;
			}

			@Override
			public boolean visit(MethodInvocation invocation) {
				List<ITypeBinding> exceptions = getExceptions(invocation.resolveMethodBinding());
				if (exceptions != null) addComment(invocation, invocation.getName(), exceptions);
				return true;
			}

			@Override
			public boolean visit(ClassInstanceCreation creation) {
				List<ITypeBinding> exceptions = getExceptions(creation.resolveConstructorBinding());
				Type type = creation.getType();
				if (exceptions != null) addComment(creation, type, exceptions);
				return true;
			}

			private List<ITypeBinding> getExceptions(IMethodBinding methodBinding) {
				return methodBinding == null ? null : Arrays.asList(methodBinding.getExceptionTypes());
			}

			private void addComment(ASTNode node, ASTNode call, List<ITypeBinding> exceptionTypes) {
				int lineNo = getLineNo(doc, ASTNodes.getExclusiveEnd(node));
				if (lineNo == -1) return;

				List<Comment> comments = commentsPerLine.getOrDefault(lineNo, new ArrayList<>(0));
				for (Comment comment : comments) {
					CommentParts parts = commentParts.computeIfAbsent(comment, c -> new CommentParts());

					if (parts.commentText == null) {
						try {
							parts.commentText = doc.get(comment.getStartPosition(), comment.getLength());
						} catch (BadLocationException e) {
							continue;
						}
					}

					List<ITypeBinding> exceptionTypesBefore = exceptionTypes;
					exceptionTypes = new ArrayList<>(exceptionTypesBefore.size());

					for (ITypeBinding exceptionType : exceptionTypesBefore) {
						boolean match = false;
						String exceptionName = exceptionType.getName();
						for(int idx = parts.commentText.indexOf(exceptionName); idx >= 0;
							idx = parts.commentText.indexOf(exceptionName, idx + exceptionName.length())) {
							if (match = addCommentException(parts.exceptionsAssociated,
									node, call, exceptionType, exceptionName, idx)) break;
						}
						if (!match) exceptionTypes.add(exceptionType);
					}
				}

				int size = exceptionTypes.size();
				if (size > 0) {
					List<ExceptionInfo> lineGhostInfos = ghostInfos.computeIfAbsent(lineNo, i -> new ArrayList<>(size));
					exceptionTypes.stream()
							.map(e -> new ExceptionInfo(doc, node, call, e))
							.forEach(lineGhostInfos::add);
				}
			}

			private boolean addCommentException(TreeMap<Integer, ExceptionInfo> exceptionsAssociated,
												ASTNode node, ASTNode call, ITypeBinding exceptionType, String exceptionName, int idx) {
				if (exceptionName.length() < 2) return false;

				// Found in comment
				Map.Entry<Integer, ExceptionInfo> entry = exceptionsAssociated.ceilingEntry(idx);
				if (entry != null && (entry.getValue() == null || entry.getKey() - idx < exceptionName.length())) return false;

				exceptionsAssociated.put(idx, new ExceptionInfo(doc, node, call, exceptionType));
				exceptionsAssociated.put(idx + exceptionName.length() - 1, null);
				return true;
			}
		});

		commentParts.forEach((comment, parts) -> {
			if (parts.exceptionsAssociated.isEmpty()) return;

			Integer[] prevEnd = new Integer[] { parts.exceptionsAssociated.ceilingKey(0) };
			List<String> stringSegments = new ArrayList<>();
			stringSegments.add(parts.commentText.substring(0, prevEnd[0]));

			parts.exceptionsAssociated.forEach((index, beginning) -> {
				if (beginning == null) {
					prevEnd[0] = index;
					return;
				}
				int endIndex = parts.exceptionsAssociated.higherKey(index) + 1;

				// Split gap between exceptions
				index -= (index - prevEnd[0]) / 2;

				// Look ahead for next one
				Integer next = parts.exceptionsAssociated.ceilingKey(endIndex);
				if (next != null) endIndex += (next - endIndex) / 2;

				stringSegments.add(parts.commentText.substring(index, endIndex));
			});

			List<ExceptionInfo> exceptions = parts.exceptionsAssociated.values().stream()
					.filter(Objects::nonNull).collect(Collectors.toList());

			// Add actual comment
			commentInfos.add(CommentInfo.create(gc, exceptions, comment.getStartPosition(), stringSegments));
		});

		ghostInfos.forEach((lineNo, exceptions) -> {
			int ghostCommentOffset = determineGhostCommentOffsetForLine(doc, lineNo);
			// Add ghost comment
			commentInfos.add(CommentInfo.create(gc, exceptions, ghostCommentOffset));
		});

		return commentInfos;
	}

	public static int getLineNo(IDocument doc, int exclusiveEnd) {
		int lineNo;
		try {
			lineNo = doc.getLineOfOffset(exclusiveEnd);
		} catch (BadLocationException e) {
			e.printStackTrace();
			lineNo = -1;
		}
		return lineNo;
	}

	public static int determineGhostCommentOffsetForLine(IDocument doc, int lineNo) {
		try {
			IRegion targetLine = doc.getLineInformation(lineNo);
			return getTrimmedEndOffset(doc, targetLine.getOffset(), targetLine.getLength());
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getTrimmedStartOffset(IDocument doc, int startOffset, int endOffset) {
		try {
			while (startOffset < endOffset && Character.isWhitespace(doc.getChar(startOffset))) startOffset++;
			return endOffset;
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	public static int getTrimmedEndOffset(IDocument doc, int startOffset, int length) {
		try {
            int endOffset = startOffset + length;
            while (endOffset > startOffset && Character.isWhitespace(doc.getChar(endOffset - 1))) endOffset--;
            return endOffset;
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
	}

	/*
	 * [mb] gets active editor, used for any action that effects the editor. The
	 * editor is where the user's code is written.
	 */
	public static IEditorPart getActiveEditor() {
		IWorkbenchWindow window= Activator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = null;
		if (window != null) {
			IWorkbenchPage page= window.getActivePage();
			if (page != null) {
				editor = page.getActiveEditor();
			}
		}
		
		return editor;
	}

	//maybe useful later? remove otherwise
	public static ITextViewer getITextViewer(IEditorPart editor)
	{
		if (editor != null) {
		    ITextOperationTarget target =
					editor.getAdapter(ITextOperationTarget.class);
		    if (target instanceof ITextViewer) {
		        return (ITextViewer)target;
		    } 
		}
		return null;
	}
}