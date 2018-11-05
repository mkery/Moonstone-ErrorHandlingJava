package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.recommend.RecManager;
import edu.cmu.moonstone.view.CatchExample;
import moonstone.ASTStatementVisitor;
import moonstone.EditorUtility;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.SuperTypeHierarchyCache;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.text.edits.*;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import recommend.RecDatabase;

import java.util.*;
import java.util.stream.Collectors;

/** Executes fixes for the given resolution. 
 * Usage: Pass in a marker, marker annotation, resolution, and tryParent, then call run()
 * @author Mary Beth Kery, Steven Moore, Michael Puskas
 */
public class QuickFix
{
	private static final boolean debug = false;
	private final ITextEditor editor;
	private final IDocument doc;
	private IMarker marker;
	private Position markerPosition;
	private Resolution resolution;

	private ASTNode selection;
	private CatchClause catchClause;
	private TryStatement tryStatement;
	private MethodDeclaration methodDeclaration;

	QuickFix(ITextEditor editor, IMarker marker, MarkerAnnotation markerAnno, Resolution resolution, ASTNode selection)
			throws BadLocationException {
		this.editor = editor;
		this.marker = marker;
		this.resolution = resolution;
		this.selection = selection;

		catchClause = EditorUtility.inCatchClause(selection);
		tryStatement = catchClause == null
				? EditorUtility.inTryStatement(selection)
				: (TryStatement) catchClause.getParent();
		methodDeclaration = EditorUtility.inMethodDeclaration(selection);

		IDocumentProvider idp = editor.getDocumentProvider();
		doc = idp.getDocument(editor.getEditorInput());
		IAnnotationModel model = idp.getAnnotationModel(editor.getEditorInput());
		this.markerPosition = model.getPosition(markerAnno);
		if (markerPosition == null)
			throw new BadLocationException("Could not determine marker position!");
	}

	public void run() 
	{
		if (resolution == null) return;
		switch(resolution)
		{
		case SPECIFIC_CATCH:
			unspecificCatchFix();
			break;
		case SEPARATE_CATCH:
			separateCatchFix();
			break;
		case REMOVE_THROW_FROM_TRY:
			removeThrowFromTryFix();
			break;
		case REMOVE_THROW_FROM_CATCH:
			removeThrowFromTryFix();
			break;
		case ADD_STACKTRACE:
			//addStrackTraceFix();
			break;
		case ADD_LOGGER:
			addLoggerFix();
			break;
		case ADD_EH_RECOMMENDATION:
			addRecommendationFix();
			break;
		case IGNOREEMPTYCATCH:
			ignoreEmptyCatch();
			break;
		case FIX_DESTRUCTIVE_WRAP:
			destructiveWrapFix();
			break;
		case TRY_WITH_RESOURCES:
			tryWithResourcesFix();
			break;
		case IGNORE:
			ignoreMarker();
			break;
		case REMOVE_CATCH:
			removeCatch();
			break;
		}
	}

	interface Change<E extends Exception> {
		void apply(IDocument doc) throws E;
	}

	interface Consumer<T, E extends Exception> {
		void accept(T t) throws E;
	}

	private static <E extends Exception> void compoundChange(IDocument doc, Change<E> change) throws E {
		IDocumentUndoManager undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(doc);
		undoManager.beginCompoundChange();
		change.apply(doc);
		undoManager.endCompoundChange();
	}

	private <E extends Exception> void applyRewrite(ASTRewrite astRewrite, ASTNode selectNodesFirst, ASTNode selectNodesLast, Consumer<TextEdit, E> textEditConsumer) throws E {
		ITrackedNodePosition trackFirst = null, trackLast = null;
		if (selectNodesFirst != null) trackFirst = astRewrite.track(selectNodesFirst);
		if (selectNodesLast != null) trackLast = astRewrite.track(selectNodesLast);

		TextEdit textEdit = astRewrite.rewriteAST(doc, null);
		textEditConsumer.accept(textEdit);

		try {
			marker.delete();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (trackFirst != null) {
			int startPosition = trackFirst.getStartPosition();
			int length = trackLast != null
					? trackLast.getStartPosition() + trackLast.getLength() - startPosition
					: trackFirst.getLength();

			EditorUtility.getITextViewer(EditorUtility.getActiveEditor()).setSelectedRange(startPosition, length);
		}
	}

	private <E extends Exception> void applyRewrite(ASTRewrite astRewrite, ASTNode selectNode, Consumer<TextEdit, E> textEditConsumer) throws E {
		applyRewrite(astRewrite, selectNode, null, textEditConsumer);
	}

	private void removeCatch() {
		IFile file = (IFile) marker.getResource();
		FileEditorInput input = new FileEditorInput(file);

		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try {
			ASTRewrite astRewrite = ASTRewrite.create(selection.getAST());

			if (catchClause != null && tryStatement.catchClauses().size() == 1
					&& tryStatement.getFinally() == null && tryStatement.resources().isEmpty()) {
				List<Statement> statements = tryStatement.getBody().statements();

				ASTNode bodyStatements = tryStatement.getLocationInParent() instanceof ChildListPropertyDescriptor
						? statements.isEmpty()
							? astRewrite.createStringPlaceholder("", ASTNode.EMPTY_STATEMENT)
							: astRewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY)
									.createMoveTarget(statements.get(0), statements.get(statements.size() - 1))
						: astRewrite.createMoveTarget(tryStatement.getBody());
				astRewrite.replace(tryStatement, bodyStatements, null);
			} else {
				ASTNode target = catchClause != null ? catchClause : ASTNodes.getTopMostType(selection);
				if (target != null && target.getLocationInParent() instanceof ChildListPropertyDescriptor)
					astRewrite.getListRewrite(target.getParent(), (ChildListPropertyDescriptor) target.getLocationInParent())
							.remove(target, null);
			}

			applyRewrite(astRewrite, null, te -> compoundChange(doc, te::apply));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	/** Adds a @SuppressWarning to the surrounding method for the given marker and removes the marker
	 *
	 */
	private void ignoreMarker()
	{
		//Get the surrounding method
		if (methodDeclaration == null) return;

		/* If we found a method parent, add the annotation */
		AST ast = selection.getAST();
		ASTRewrite astRewrite = ASTRewrite.create(ast);

			/* Check if there is already an annotation to add ours to */
		SingleMemberAnnotation existingAnnotation = null;

		List<IExtendedModifier> modList = methodDeclaration.modifiers();
		for(IExtendedModifier mod : modList)
            if(mod instanceof SingleMemberAnnotation)
                existingAnnotation = (SingleMemberAnnotation) mod;

		if(existingAnnotation != null) //If we have found an existing annotation to add ours to
        {
            try{
                /* Generate the annotation */
                SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();

                //Set the type of annotation that it is
                annotation.setTypeName(ast.newSimpleName("SuppressWarnings"));

                /* Build the new annotation value */
                String generalID = MarkerManager.getGeneralID(marker.getType()); //ID of the marker

                //Create an array and add the old values to it
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

                //Create the new value and add it to the array
                StringLiteral valueLiteral = ast.newStringLiteral();
                valueLiteral.setLiteralValue(generalID);
                valueArr.expressions().add(valueLiteral);

                //Add the array to the new annotation
                annotation.setValue(valueArr);

                /* Update the method's annotation */
                ListRewrite listRewrite = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
                listRewrite.replace(existingAnnotation, annotation, null);

				applyRewrite(astRewrite, selection, te -> compoundChange(doc, te::apply));
            }
            catch (CoreException | MalformedTreeException | BadLocationException e)
            {
                e.printStackTrace();
            }
        }
        else //Else, make our own annotation and add it to the method
        {
            /* Generate the annotation, and add it to the method */
            try
            {
                /* Generate the annotation */
                SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
                String generalID = MarkerManager.getGeneralID(marker.getType());

                //Set the type of annotation that it is
                annotation.setTypeName(ast.newSimpleName("SuppressWarnings"));

                //Set the annotation value
                StringLiteral valueLiteral = ast.newStringLiteral();
                valueLiteral.setLiteralValue(generalID);
                annotation.setValue(valueLiteral);

                /* Add the annotation to the method */
                ListRewrite listRewrite = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.MODIFIERS2_PROPERTY);
                listRewrite.insertAt(annotation, 0, null);

				applyRewrite(astRewrite, selection, te -> compoundChange(doc, te::apply));
            }
            catch (CoreException | MalformedTreeException | BadLocationException e)
            {
                e.printStackTrace();
            }
        }
	}

	private void unspecificCatchFix()
	{
		//Replace the catch's argument with the list of proper exceptions
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		//Get the doc that we'll be modifying
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try
        {
			//Get the list of proper exceptions
			List<ITypeBinding> exceptionsThrown = Optional.ofNullable(tryStatement)
					.map(EditorUtility::getExceptionsThrownIn)
					.orElseGet(() -> EditorUtility.getExceptionsThrownIn(methodDeclaration));
			List<ITypeBinding> exceptionRoots = reduceToSuperClasses(exceptionsThrown);

            //Check if there are any
            if(exceptionRoots.size() == 0) return; //Return if there's nothing to base the resolution on

            if (catchClause != null) {
				AST ast = catchClause.getAST();

				CompilationUnit root = (CompilationUnit) catchClause.getRoot();
				ImportRewrite importRewrite = ImportRewrite.create(root, true);

				UnionType unionType = ast.newUnionType();
				exceptionRoots.stream().map(binding -> importRewrite.addImport(binding, ast)).forEach(unionType.types()::add);

				TextEdit textEdit = importRewrite.rewriteImports(null);

				ASTNode selectNode;
				ASTRewrite astRewrite = ASTRewrite.create(ast);
				astRewrite.set(selectNode = catchClause.getException(), SingleVariableDeclaration.TYPE_PROPERTY, unionType, null);

				applyRewrite(astRewrite, selectNode, te -> {
					textEdit.addChild(te);
					compoundChange(doc, textEdit::apply);
				});
			} else if (methodDeclaration != null) {
				AST ast = methodDeclaration.getAST();

				CompilationUnit root = (CompilationUnit) methodDeclaration.getRoot();
				ASTNode foundNode = NodeFinder.perform(root, markerPosition.offset, markerPosition.length);
				Type exception = ASTNodes.getTopMostType(foundNode);

				ImportRewrite importRewrite = ImportRewrite.create(root, true);
				ASTRewrite astRewrite = ASTRewrite.create(ast);
				ListRewrite listRewrite = astRewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);

				List<Type> thrownExceptionTypes = methodDeclaration.thrownExceptionTypes();
				List<ITypeBinding> thrownExceptionBindings = thrownExceptionTypes.stream().map(Type::resolveBinding).collect(Collectors.toList());

				Type firstNewThrows = null, lastNewThrows = null;
				for (int i = exceptionRoots.size() - 1; i >= 0; i--) {
					ITypeBinding binding = exceptionRoots.get(i);
					if (thrownExceptionBindings.stream().noneMatch(binding::isEqualTo)) {
						firstNewThrows = importRewrite.addImport(binding, ast);
						if (lastNewThrows == null) lastNewThrows = firstNewThrows;
						listRewrite.insertAfter(firstNewThrows, exception, null);
					}
				}
				listRewrite.remove(exception, null);

				TextEdit textEdit = importRewrite.rewriteImports(null);
				applyRewrite(astRewrite, firstNewThrows, lastNewThrows, te -> {
					textEdit.addChild(te);
					compoundChange(doc, textEdit::apply);
				});
			}
        }
        catch (BadLocationException | CoreException e)
        {
            e.printStackTrace();
        }
	}

	public static int superClassCompare(ITypeBinding tb1, ITypeBinding tb2) {
		try {
			IType t1 = (IType) tb1.getJavaElement();
			IType t2 = (IType) tb2.getJavaElement();
			ITypeHierarchy typeHierarchyT1 = SuperTypeHierarchyCache.getTypeHierarchy(t1);
			if (Arrays.asList(typeHierarchyT1.getAllSuperclasses(t1)).contains(t2)) {
				return -1;
			}
			ITypeHierarchy typeHierarchyT2 = SuperTypeHierarchyCache.getTypeHierarchy(t2);
			if (Arrays.asList(typeHierarchyT2.getAllSuperclasses(t2)).contains(t1)) {
				return 1;
			}
			return 0;
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	public static void dfs(IType type, Map<IType, Set<IType>> graph, List<IType> sorted) {
		for (IType subt : graph.getOrDefault(type, Collections.emptySet())) 
			dfs(subt, graph, sorted);
		sorted.add(type);
	}

	public static List<ITypeBinding> sortSuperClasses(List<ITypeBinding> typeBindings) {
		try {
			IType exception = null;

			Map<IType, Set<IType>> graph = new HashMap<>();
			for (ITypeBinding tb : typeBindings) {
				IType t = (IType) tb.getJavaElement();
				ITypeHierarchy h = SuperTypeHierarchyCache.getTypeHierarchy(t);

				IType[] supertypes = h.getAllSupertypes(t);
				
				if (exception == null) {
					for (IType supertype : supertypes) {
						if (!supertype.getFullyQualifiedName().equals(Exception.class.getName())) continue;
						exception = supertype;
						break;
					}
				} 
						
				for (IType supt : supertypes) {
					graph.computeIfAbsent(supt, supt_ -> new HashSet<>()).add(t);
					t = supt;
				}
			}

			List<ITypeBinding> sorted = new ArrayList<>();
			List<IType> sortedTypes = new ArrayList<>();
			dfs(exception, graph, sortedTypes);

			for (IType sortedType : sortedTypes) {	
				for (ITypeBinding typeBinding : typeBindings) {
					if (!sortedType.equals(typeBinding.getJavaElement())) continue;
					sorted.add(typeBinding);
					break;
				}
			}

			return sorted;
		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<ITypeBinding> reduceToSuperClasses(List<ITypeBinding> typeBindings) {
		List<ITypeBinding> exceptionsThrown = new ArrayList<>();
		typeBindings.stream()
				.forEach(t -> {
					ListIterator<ITypeBinding> iterator = exceptionsThrown.listIterator();
					IType t1 = (IType) t.getJavaElement();
					while (iterator.hasNext()) {
						IType t2 = (IType) iterator.next().getJavaElement();
						try {
							if (debug) System.out.format("checking %s against existing %s: ", t1.getFullyQualifiedName(), t2.getFullyQualifiedName());
							ITypeHierarchy typeHierarchyT1 = SuperTypeHierarchyCache.getTypeHierarchy(t1);
							if (t1 == t2 || Arrays.asList(typeHierarchyT1.getAllSuperclasses(t1)).contains(t2)) {
								if (debug) System.out.format("not adding %s\n", t1.getFullyQualifiedName());
								return;
							}
							ITypeHierarchy typeHierarchyT2 = SuperTypeHierarchyCache.getTypeHierarchy(t2);
							if (Arrays.asList(typeHierarchyT2.getAllSuperclasses(t2)).contains(t1)) {
								if (debug) System.out.format("removing %s\n", t2.getFullyQualifiedName());
								iterator.remove();
							} else if (debug) System.out.println();
						} catch (JavaModelException e) {
							e.printStackTrace();
						}
					}
					if (debug) System.out.format("adding %s\n", t1);
					iterator.add(t);
				});
		return exceptionsThrown;
	}

	private void separateCatchFix()
	{
		if (catchClause == null) return;
		/* Find the offending catch clause */

		//Replace the catch's argument with the list of proper exceptions
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		//Get the doc that we'll be modifying
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try
        {
			List<CatchClause> catchClauses = tryStatement.catchClauses();
			catchClauses = catchClauses.stream()
					.filter(c -> c != catchClause)
					.collect(Collectors.toList());
			List<ITypeBinding> existingCatchTypes = catchClauses.stream()
					.map(CatchClause::getException)
					.map(SingleVariableDeclaration::getType)
					.map(Type::resolveBinding)
					.collect(Collectors.toList());

			//Get the list of proper exceptions
			List<ITypeBinding> exceptionsIn = EditorUtility.getExceptionsThrownIn(tryStatement).stream()
					.filter(typeBinding -> existingCatchTypes.stream().noneMatch(typeBinding::isEqualTo))
					.collect(Collectors.toList());

			//Check if there are any
            if(exceptionsIn.isEmpty()) return; //Return if there's nothing to base the resolution on
			
			List<ITypeBinding> exceptionsThrown = sortSuperClasses(exceptionsIn);

			CompilationUnit root = (CompilationUnit) catchClause.getRoot();
			ImportRewrite importRewrite = ImportRewrite.create(root, true);
			ASTRewrite astRewrite = ASTRewrite.create(catchClause.getAST());
			ListRewrite listRewrite = astRewrite.getListRewrite(tryStatement, TryStatement.CATCH_CLAUSES_PROPERTY);
			listRewrite.remove(catchClause, null);

			ASTNode selectNode = null;
			int ectIdx = 0;
			for (int i = 0; i < exceptionsThrown.size(); i++) {
				ITypeBinding binding = exceptionsThrown.get(i);
				Type type = importRewrite.addImport(binding, astRewrite.getAST());
				CatchClause newCatchClause = astRewrite.getAST().newCatchClause();
				SingleVariableDeclaration exception = ASTNodes.copySubtree(astRewrite.getAST(), catchClause.getException());
				exception.setType(type);
				newCatchClause.setException(exception);

				Block catchClauseBody = catchClause.getBody();
				List<Statement> statements = catchClauseBody.statements();
				Optional<Statement> first = statements.stream().findFirst();
				Optional<Statement> last = first.map(f -> statements.stream().skip(statements.size() - 1).findFirst().get());

				ListRewrite statementsRewrite = astRewrite.getListRewrite(catchClauseBody, Block.STATEMENTS_PROPERTY);
				ListRewrite newStatementsRewrite = astRewrite.getListRewrite(newCatchClause.getBody(), Block.STATEMENTS_PROPERTY);

				ASTNode comment = astRewrite.createStringPlaceholder("// TODO Changes might be necessary for this handler", ASTNode.EMPTY_STATEMENT);
				newStatementsRewrite.insertFirst(comment, null);
				if (i == 0) selectNode = comment;

				boolean isFirstCatch = i == 0;
				first.ifPresent(f -> newStatementsRewrite.insertLast(isFirstCatch
                        ? statementsRewrite.createMoveTarget(f, last.get())
                        : statementsRewrite.createCopyTarget(f, last.get()), null));

				ASTNode insertBeforeNode = null;
				for (; ectIdx < existingCatchTypes.size(); ectIdx++) {
					CatchClause clause = catchClauses.get(ectIdx);
					ITypeBinding ect = existingCatchTypes.get(ectIdx);
					if (clause != catchClause && superClassCompare(binding, ect) < 0) {
						insertBeforeNode = clause;
						break;
					}
				}

				if (insertBeforeNode != null) listRewrite.insertBefore(newCatchClause, insertBeforeNode, null);
				else listRewrite.insertLast(newCatchClause, null);
			}

			TextEdit textEdit = importRewrite.rewriteImports(null);
			applyRewrite(astRewrite, selectNode, te -> {
				textEdit.addChild(te);
				compoundChange(doc, textEdit::apply);
			});
        }
        catch (BadLocationException | CoreException e)
        {
            e.printStackTrace();
        }
	}

	private void addLoggerFix()
	{
		IFile file = (IFile) marker.getResource();

		Block node;
		Pair<CatchClause, Block> nodePair = getCatchOrFinally(selection);
		List<CatchExample[]> catchExamples;
		if (nodePair.getLeft() != null) {
			node = nodePair.getLeft().getBody();
			catchExamples = RecDatabase.retrieveMatches(
					Collections.singletonList(nodePair.getLeft().getException().getType().resolveBinding().getQualifiedName()),
					file.getProject(), file, markerPosition);
		} else if ((node = nodePair.getRight()) != null) {
			catchExamples = RecDatabase.retrieveMatches(file.getProject(), file, markerPosition);
		} else return;

		if (catchExamples.stream().anyMatch(cs -> cs[0].isLog())) {
			RecManager.getActive(editor)
					.showCustomView(markerPosition.offset, markerPosition.length, true, false);
			return;
		}

		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ASTNode selectNode = astRewrite.createStringPlaceholder("// TODO Add a logger here", ASTNode.EMPTY_STATEMENT);
		astRewrite.getListRewrite(node, Block.STATEMENTS_PROPERTY)
				.insertLast(selectNode, null);

		try {
			applyRewrite(astRewrite, selectNode, te -> compoundChange(doc, te::apply));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void addRecommendationFix() {
		RecManager.getActive(editor)
				.showCustomView(markerPosition.offset, markerPosition.length, false, true);
	}

	public static Pair<CatchClause, Block> getCatchOrFinally(ASTNode selectedNode) {
		MutablePair<CatchClause, Block> nodePair = MutablePair.of(null, null);
		EditorUtility.acceptParents(selectedNode, new ASTStatementVisitor() {
            @Override
            public boolean visit(Block node) {
                if (TryStatement.FINALLY_PROPERTY.equals(node.getLocationInParent())) {
                    nodePair.setRight(node);
                    return false;
                }
                return true;
            }
            @Override
            public boolean visit(CatchClause node) {
                nodePair.setLeft(node);
                return false;
            }
        });
		return nodePair;
	}

	private void ignoreEmptyCatch() {
		if (catchClause == null) return;
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try
        {
			//Determine the name of the preferred variable
			SimpleName name = catchClause.getException().getName();
			ASTRewrite astRewrite = ASTRewrite.create(name.getAST());
			ASTNode selectNode = astRewrite.createStringPlaceholder("ignored", ASTNode.SIMPLE_NAME);
			astRewrite.set(name, SimpleName.IDENTIFIER_PROPERTY, selectNode, null);

			applyRewrite(astRewrite, selectNode, te -> compoundChange(doc, te::apply));
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
	}
	
	private void removeThrowFromTryFix()
	{
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try
        {
			ASTRewrite astRewrite = ASTRewrite.create(selection.getAST());
			if (selection.getLocationInParent() instanceof ChildListPropertyDescriptor)
				astRewrite.remove(selection, null);
			else
				astRewrite.replace(selection, astRewrite.getAST().newBlock(), null);

			applyRewrite(astRewrite, selection.getParent(), te -> compoundChange(doc, te::apply));
        }
        catch (BadLocationException e)
        {
            e.printStackTrace();
        }
	}

	@SuppressWarnings("restriction")
	private void destructiveWrapFix()
	{
		if (catchClause == null) return;
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		try {
			//Determine the name of the preferred variable
			if (selection.getNodeType() != ASTNode.CLASS_INSTANCE_CREATION) return;
			ClassInstanceCreation instanceCreation = (ClassInstanceCreation) selection; //Get the ClassInstanceCreation

			Optional<int[]> mapping = EditorUtility.getExceptionParameterMapping(catchClause.getException(), instanceCreation);
			if (mapping.isPresent()) {
				ASTRewrite astRewrite = ASTRewrite.create(instanceCreation.getAST());
				int[] m = mapping.get();

				List<Expression> expressionList = instanceCreation.arguments();
				Expression[] oldArgs = expressionList.toArray(new Expression[m.length]);
				List<Expression> args = expressionList.stream().map(a -> ASTNodes.createMoveTarget(astRewrite, a))
						.collect(Collectors.toList());

				ListRewrite listRewrite = astRewrite.getListRewrite(instanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
				listRewrite.insertLast(oldArgs[oldArgs.length - 1] = (Expression) astRewrite.createStringPlaceholder("null", ASTNode.SIMPLE_NAME), null);

				for (int i = 0; i < args.size(); i++) {
					listRewrite.replace(oldArgs[m[i]], args.get(i), null);
				}
				SimpleName selectNode = ASTNodes.copySubtree(astRewrite.getAST(), catchClause.getException().getName());
				listRewrite.replace(oldArgs[m[m.length - 1]], selectNode, null);

				applyRewrite(astRewrite, selectNode, te -> compoundChange(doc, te::apply));
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private MethodInvocation getCloseInvocation(SimpleName name) {
		if (!MethodInvocation.EXPRESSION_PROPERTY.equals(name.getLocationInParent())) return null;
		MethodInvocation invocation = (MethodInvocation) name.getParent();
		if (!"close".equals(invocation.getName().getIdentifier())) return null;
		return invocation;
	}

	private void removeCloseInvocations(ASTRewrite astRewrite, ASTNode root, IBinding variableBinding) {
		root.accept(new ASTStatementVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				IBinding binding = node.resolveBinding();
				if (binding == null || !variableBinding.isEqualTo(binding)) return false;
				MethodInvocation closeInvocation = getCloseInvocation(node);
				if (closeInvocation == null) return false;

				ExpressionStatement statement = (ExpressionStatement) closeInvocation.getParent();
				astRewrite.getListRewrite(statement.getParent(), (ChildListPropertyDescriptor) statement.getLocationInParent())
						.remove(statement, null);

				return false;
			}
		});
	}
	
	private void tryWithResourcesFix()
	{
		FileEditorInput input = new FileEditorInput((IFile) marker.getResource());
		IDocument doc = JavaUI.getDocumentProvider().getDocument(input);

		//Determine the name of the preferred variable
		ASTNode node = selection;

		if (node == null) return;
		if (node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
			node = ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
		if (fragment == null) return;

		/* Retrieve the variable name of the resource to close */
		IBinding variableBinding = fragment.getName().resolveBinding();

		/* Use the varName to check if the statement is a declaration or an expression */
		ASTNode declarationStatement = fragment.getParent();
		TryStatement tryStatement = Optional.ofNullable(this.tryStatement)
				// only consider immediate try parents
				.filter(t -> declarationStatement.getParent() == t.getBody()
						&& Block.STATEMENTS_PROPERTY.equals(declarationStatement.getLocationInParent()))
				.orElseGet(() -> {
						// Search for next try statement
						MutablePair<TryStatement,Boolean> result = MutablePair.of(null, false);
						methodDeclaration.getBody().accept(new ASTStatementVisitor() {
							boolean yieldNext = false;
							@Override
							public void endVisit(VariableDeclarationFragment node) {
								if (node == fragment) yieldNext = true;
							}
							@Override
							public boolean visit(SimpleName node) {
								IBinding binding = node.resolveBinding();
								if (variableBinding.isEqualTo(binding)) result.setRight(true);
								return !yieldNext;
							}
							@Override
							public boolean visit(TryStatement node) {
								if (yieldNext && result.getLeft() == null) result.setLeft(node);
								return !yieldNext;
							}
						});

						return result.getRight() ? result.getLeft() : null;
					});

		ASTRewrite astRewrite = ASTRewrite.create(fragment.getAST());
		try {
			ASTNode selectNode;
			if(tryStatement == null) {
				ASTNode blockNode = declarationStatement, childNode = null;
				for (; blockNode != null && !(blockNode instanceof Block); blockNode = blockNode.getParent())
					childNode = blockNode;
				if (childNode == null || blockNode == null) return;
				Block block = (Block) blockNode;

				removeCloseInvocations(astRewrite, block, variableBinding);

				tryStatement = astRewrite.getAST().newTryStatement();
				ListRewrite newListRewrite = astRewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
				ListRewrite listRewrite = astRewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

				List<Statement> blockStatements = listRewrite.getRewrittenList();
				int childIndex = blockStatements.indexOf(childNode);
				if (childIndex >= 0 && childIndex < blockStatements.size() - 1) {
					Statement from = blockStatements.get(childIndex + 1);
					Statement to = blockStatements.get(blockStatements.size() - 1);

					newListRewrite.insertLast(listRewrite.createMoveTarget(from, to, tryStatement, null), null);
				} else {
					newListRewrite.insertLast(astRewrite.createStringPlaceholder(
							String.format("// use %s here", fragment.getName().getIdentifier()), ASTNode.EMPTY_STATEMENT), null);
					listRewrite.insertAfter(tryStatement, childNode, null);
				}
			} else removeCloseInvocations(astRewrite, tryStatement, variableBinding);

			if(fragment.getInitializer() != null && !ASTNodes.isLiteral(fragment.getInitializer())) //If the statement is a declaration
			{
				VariableDeclarationExpression declarationExpression = popDeclarationFragment(astRewrite, fragment);
				astRewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY)
						.insertLast(selectNode = declarationExpression, null);
			}
			else //Else the statement is an expression
			{
				if (fragment.getInitializer() == null) {
					astRewrite.set(fragment, fragment.getInitializerProperty(), astRewrite.getAST().newNullLiteral(), null);
				}

				Block finallyBlock = tryStatement.getFinally();
				if (finallyBlock == null) {
					finallyBlock = astRewrite.getAST().newBlock();
					astRewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBlock, null);
				}

				MethodInvocation methodInvocation = astRewrite.getAST().newMethodInvocation();
				methodInvocation.setExpression(ASTNodes.copySubtree(astRewrite.getAST(), fragment.getName()));
				methodInvocation.setName((SimpleName) astRewrite.createStringPlaceholder("close", ASTNode.SIMPLE_NAME));
				ExpressionStatement methodStatement = astRewrite.getAST().newExpressionStatement(methodInvocation);

				IfStatement ifStatement = astRewrite.getAST().newIfStatement();
				InfixExpression infixExpression = astRewrite.getAST().newInfixExpression();
				infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
				infixExpression.setLeftOperand(ASTNodes.copySubtree(astRewrite.getAST(), fragment.getName()));
				infixExpression.setRightOperand(astRewrite.getAST().newNullLiteral());
				ifStatement.setExpression(infixExpression);
				ifStatement.setThenStatement(methodStatement);

				astRewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY)
						.insertLast(selectNode = ifStatement, null);
			}

			applyRewrite(astRewrite, selectNode, te -> compoundChange(doc, te::apply));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private static VariableDeclarationExpression popDeclarationFragment(ASTRewrite astRewrite, VariableDeclarationFragment fragment) {
		VariableDeclarationStatement declarationStatement = (VariableDeclarationStatement) fragment.getParent();
		VariableDeclarationExpression declarationExpression = astRewrite.getAST()
				.newVariableDeclarationExpression((VariableDeclarationFragment) astRewrite.createMoveTarget(fragment));

		declarationExpression.modifiers().addAll(ASTNode.copySubtrees(astRewrite.getAST(), declarationStatement.modifiers()));
		declarationExpression.setType((Type) ASTNode.copySubtree(astRewrite.getAST(), declarationStatement.getType()));

		if (declarationStatement.fragments().size() == 1) {
            astRewrite.remove(declarationStatement, null);
        } else {
            astRewrite.getListRewrite(declarationStatement, VariableDeclarationStatement.FRAGMENTS_PROPERTY)
                    .remove(fragment, null);
        }
		return declarationExpression;
	}
}