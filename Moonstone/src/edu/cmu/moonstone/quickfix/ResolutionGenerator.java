package edu.cmu.moonstone.quickfix;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.view.CatchExample;
import moonstone.*;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import recommend.RecDatabase;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/** Generates resolutions, descriptions, and situations for the various parts of the quickfix system
 * 
 * @author Michael Puskas
 */
public class ResolutionGenerator 
{
	/** Returns an array of ResolutionInfo objects relevant to the given marker, and sets the problemDescription
	 * 
	 * @param model
	 * @param markerAnnotation
	 * @return
	 */
	public List<ResolutionInfo> getResolutions(IAnnotationModel model, MarkerAnnotation markerAnnotation, TryStatement tryParent, ASTNode node) {
		List<ITypeBinding> exceptionsThrown = EditorUtility.getExceptionsThrownIn(tryParent);
		return getResolutions(model, markerAnnotation, exceptionsThrown, tryParent, node);
	}
	public List<ResolutionInfo> getResolutions(IAnnotationModel model, MarkerAnnotation markerAnnotation, ASTNode node) {
		MethodDeclaration methodDeclaration = EditorUtility.inMethodDeclaration(node);
		List<ITypeBinding> exceptionsThrown = EditorUtility.getExceptionsThrownIn(methodDeclaration);
		return getResolutions(model, markerAnnotation, exceptionsThrown, methodDeclaration, node);
	}

	private List<ResolutionInfo> getResolutions(IAnnotationModel model, MarkerAnnotation markerAnnotation,
												List<ITypeBinding> exceptionsThrown, ASTNode parent, ASTNode node) {
		IMarker marker = markerAnnotation.getMarker();
		Position position = model.getPosition(markerAnnotation);
		IFile file = (IFile) marker.getResource();
		try {
			String type = marker.getType();
			return getResolutions(file, type, exceptionsThrown, parent, node, position);
		} catch (CoreException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	public List<ResolutionInfo> getResolutions(IFile file, String markerType, List<ITypeBinding> exceptionsThrown, ASTNode parent, ASTNode node, Position position) {
		String parentType = MarkerManager.getParentID(markerType);

		List<ResolutionInfo> resInfo = new ArrayList<>();
		if(parentType.equals(MarkerDef.BAD_CATCH_MARKER))
		{	
			//Check if there are any exceptions being thrown in the try body
			if(parent.getNodeType() != ASTNode.METHOD_DECLARATION) //If there are multiple exceptions, give them both options
			{
				CatchClause catchClause = EditorUtility.inCatchClause(node);
				List<CatchClause> catchClauses = ((TryStatement) parent).catchClauses();
				for (CatchClause prevClause : catchClauses) {
					if (prevClause == catchClause) break;

					Type declType = prevClause.getException().getType();
					if (declType.isUnionType()) {
						List<Type> types = ((UnionType) declType).types();
						types.stream()
								.map(Type::resolveBinding)
								.forEach(binding -> exceptionsThrown.removeIf(eBinding -> eBinding.isAssignmentCompatible(binding)));
					} else {
						ITypeBinding binding = declType.resolveBinding();
						exceptionsThrown.removeIf(eBinding -> eBinding.isAssignmentCompatible(binding));
					}
				}

				if(exceptionsThrown.size() > 1) //If there are multiple exceptions, give them both options
                {
                    ResolutionInfo resInfo1 = new ResolutionInfo();
                    resInfo.add(resInfo1);
                    Iterable<String> exceptionNames = exceptionsThrown.stream().map(ITypeBinding::getName)::iterator;
                    resInfo1.setProposition(String.format("Build separate handlers for %s", String.join(", ", exceptionNames)));
                    resInfo1.setResolution(Resolution.SEPARATE_CATCH);
                }
			}
			
			//Always initialize the first option
			ResolutionInfo resInfo0 = new ResolutionInfo();

			//If there are 0 exceptions, prompt them to add more
			if(exceptionsThrown.size() == 0) {
				String proposition = parent.getNodeType() != ASTNode.METHOD_DECLARATION
						? "Remove unnecessary catch handler"
						: "Remove unnecessary throws declaration";
				resInfo0.setProposition(proposition);
				resInfo0.setResolution(Resolution.REMOVE_CATCH);
				resInfo.add(resInfo0);
			}
			else //Else, let them auto-fill the appropriate exceptions
			{
				List<ITypeBinding> exceptionRoots = QuickFix.reduceToSuperClasses(exceptionsThrown);
				if (exceptionRoots.stream().anyMatch(t -> ExceptionVisitor.getCommonExceptionProblem(t) == Problem.NONE)) {
					Iterable<String> exceptionNames = exceptionRoots.stream().map(ITypeBinding::getName)::iterator;
					String proposition = parent.getNodeType() != ASTNode.METHOD_DECLARATION
							? exceptionRoots.size() > 1
                                ? String.format("Turn into combined catch handler for %s", String.join(" | ", exceptionNames))
                                : String.format("Narrow caught exceptions to %s", String.join(" | ", exceptionNames))
							: String.format("Narrow declared exceptions to %s", String.join(", ", exceptionNames));
					resInfo0.setProposition(proposition);
					resInfo0.setResolution(Resolution.SPECIFIC_CATCH);
					resInfo.add(resInfo0);
				}
			}
		}
		else if(parentType.equals(MarkerDef.BAD_THROWFROMTRY_MARKER))
		{
			ResolutionInfo resInfo0 = new ResolutionInfo();
			resInfo.add(resInfo0);
			resInfo0.setProposition("Suggestion: Create a custom application exception");

			ResolutionInfo resInfo1 = new ResolutionInfo();
			resInfo.add(resInfo1);
			resInfo1.setProposition("Remove throw statement");
			resInfo1.setResolution(Resolution.REMOVE_THROW_FROM_TRY);
		}
		else if(parentType.equals(MarkerDef.BAD_THROWFROMCATCH_MARKER))
		{
			ResolutionInfo resInfo0 = new ResolutionInfo();
			resInfo.add(resInfo0);
			resInfo0.setProposition("Suggestion: Create a custom application exception");

			ResolutionInfo resInfo1 = new ResolutionInfo();
			resInfo.add(resInfo1);
			resInfo1.setProposition("Remove throw statement");
			resInfo1.setResolution(Resolution.REMOVE_THROW_FROM_CATCH);
		}
		else if(markerType.equals(MarkerDef.EMPTY_CATCH_MARKER))
		{
			Pair<CatchClause, Block> nodePair = QuickFix.getCatchOrFinally(node);
			if (nodePair.getLeft() == null && nodePair.getRight() == null) return resInfo;
			
			List<CatchExample[]> catchExamples = RecDatabase.retrieveMatches(
					Collections.singletonList(nodePair.getLeft().getException().getType().resolveBinding().getQualifiedName()),
					file.getProject(), file, position);
			Optional<CatchExample[]> logExample = catchExamples.stream().filter(cs -> cs[0].isLog())
					.findFirst();
			Optional<CatchExample[]> nonLogExample = catchExamples.stream().filter(cs -> !cs[0].isLog())
					.findFirst();

			nonLogExample.ifPresent(ce -> {
				ResolutionInfo resInfoOtherEH = new ResolutionInfo();
				resInfo.add(resInfoOtherEH);
				resInfoOtherEH.setProposition("Suggestion: Add handler from recommendations (jump to match)");
				resInfoOtherEH.setResolution(Resolution.ADD_EH_RECOMMENDATION);
			});

			ResolutionInfo resInfoLog = new ResolutionInfo();
			resInfo.add(resInfoLog);
			resInfoLog.setProposition(logExample.isPresent()
					? "Suggestion: Add logging from recommendations (jump to match)"
					: "Suggestion: Add a logger (create TODO)");
			resInfoLog.setResolution(Resolution.ADD_LOGGER);

			/*ResolutionInfo resInfo1 = new ResolutionInfo();
			resInfo.add(resInfo1);
			resInfo1.setProposition("Add stack trace");
			resInfo1.setResolution(Resolution.ADD_STACKTRACE);*/

			ResolutionInfo resInfo2 = new ResolutionInfo();
			resInfo.add(resInfo2);
			resInfo2.setProposition("Ignore this catch handler");
			resInfo2.setResolution(Resolution.IGNOREEMPTYCATCH);
		}
		else if(markerType.equals(MarkerDef.DESTRUCTIVE_WRAP_MARKER))
		{
			ResolutionInfo resInfo0 = new ResolutionInfo();
			resInfo.add(resInfo0);
			resInfo0.setProposition("Add original exception to thrown exception");
			resInfo0.setResolution(Resolution.FIX_DESTRUCTIVE_WRAP);
		}
		else if(markerType.equals(MarkerDef.RESOURCE_LEAK_MARKER))
		{
			if (node.getNodeType() != ASTNode.VARIABLE_DECLARATION_FRAGMENT)
				node = ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) node;
			
			boolean isTryWithResources = fragment != null 
					&& fragment.getInitializer() != null 
					&& !ASTNodes.isLiteral(fragment.getInitializer());
			
			ResolutionInfo resInfo0 = new ResolutionInfo();
			resInfo.add(resInfo0);
			String proposition = isTryWithResources 
					? "Automatically close resource with try-with-resources statement"
					: "Close resource in finally block";
			resInfo0.setProposition(proposition);
			resInfo0.setResolution(Resolution.TRY_WITH_RESOURCES);
		}

		return resInfo;
	}
	
	/** Returns the proper description for the given moonstone marker
	 * 
	 * @return
	 */
	public Description getDescription(IMarker marker)
	{
		String origType = "";

		try
		{
			origType = marker.getType();
		}
		catch(CoreException e)
		{
			e.getStackTrace();
		}

		return UserMessage.createDescription(origType);
	}

	/** Returns a list of the situations that reach the catch block that the marker is attached to
	 * Also adds highlighting to the parts of code that cause each statement
	 * 
	 * @param editor
	 * @param markerAnno The annotation to use for obtaining the offending clause's position
	 * @param tryParent The TryStatement to get situations for
	 * @return A list of the potential exceptional situations that occur in tryParent and are caught in the marker's catch block
	 */
	public List<SituationInfo> getSituations(ITextEditor editor, MarkerAnnotation markerAnno, TryStatement tryParent)
	{
		if (tryParent == null) return Collections.emptyList();

		//Find the marker start
		IDocumentProvider idp = editor.getDocumentProvider();
		IAnnotationModel model = idp.getAnnotationModel(editor.getEditorInput());
		Position markerPosition = model.getPosition(markerAnno);

		ASTNode selection = NodeFinder.perform(tryParent, markerPosition.offset, markerPosition.length);

		/* Find the offending catch clause */
		CatchClause offendingClause = EditorUtility.inCatchClause(selection);

		List<ITypeBinding> caughtBeforeTypes = new ArrayList<>();
		List<CatchClause> catchClauses = tryParent.catchClauses();
		for (CatchClause catchClause : catchClauses) {
			if (catchClause == offendingClause) break;
			caughtBeforeTypes.addAll(resolveCaughtTypes(catchClause.getException().getType()));
		}

		/* Compare the caught exceptions to the possibly thrown exceptions, add any matches to the situationList */
		//Fill typeList with the caught exceptions
		List<ITypeBinding> caughtTypes = resolveCaughtTypes(offendingClause.getException().getType());

		List<Pair<ASTNode, List<ITypeBinding>>> exceptions = EditorUtility.checkForQualifiedExceptions(tryParent.getBody());
		exceptions.forEach(p -> p.getRight().removeIf(thrown ->
				caughtBeforeTypes.stream().anyMatch(thrown::isAssignmentCompatible)
						|| caughtTypes.stream().noneMatch(thrown::isAssignmentCompatible)));

		CompilationUnit compilationUnit = (CompilationUnit) tryParent.getRoot();
		Map<Annotation, Position> annotations = new HashMap<>();

		List<SituationInfo> situationInfos = exceptions.stream()
				.filter(p -> !p.getRight().isEmpty())
				.map(p -> {
					List<ITypeBinding> thrownExceptions = p.getRight();
					ASTNode node = p.getLeft();

					int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
					Position position = new Position(node.getStartPosition(), node.getLength());

					//Create the annotation and give it an appropriate position
					Annotation highlightAnno = new Annotation(MarkerDef.GREY_HIGHLIGHT, false, "");
					//Add the highlight annotation to the model
					annotations.put(highlightAnno, position);

					String situationDescription;
					DESCRIPTION:
					{
						IMethodBinding methodBinding;

						switch (node.getNodeType()) {
							case ASTNode.THROW_STATEMENT:
								//Build the situation description
								situationDescription = String.format("%s is explicitly thrown on line %d.", thrownExceptions.get(0).getName(), lineNumber);
								break DESCRIPTION;
							case ASTNode.METHOD_INVOCATION:
								methodBinding = ((MethodInvocation) node).resolveMethodBinding();
								break;
							case ASTNode.CONSTRUCTOR_INVOCATION:
								methodBinding = ((ConstructorInvocation) node).resolveConstructorBinding();
								break;
							default:
								throw new UnsupportedOperationException();
						}

						//Build the situation description
						StringBuilder thrownNames = new StringBuilder();
						for (String thrownName : (Iterable<String>) thrownExceptions.stream()
								.map(ITypeBinding::getName)
								.limit(thrownExceptions.size() - 1)::iterator) {
							thrownNames.append(thrownName);
							thrownNames.append(", ");
						}
						if (thrownExceptions.size() > 1) thrownNames.append("or ");
						thrownNames.append(thrownExceptions.get(thrownExceptions.size() - 1).getName());

						situationDescription = String.format("If %s() fails, %s may be thrown on line %d.",
								methodBinding.getName(), thrownNames, lineNumber);
					}

					return new SituationInfo(situationDescription, highlightAnno);
				})
				.collect(Collectors.toList());

		IAnnotationModelExtension modelEx = (IAnnotationModelExtension) model;
		modelEx.replaceAnnotations(null, annotations);

		return situationInfos;
	}

	List<ITypeBinding> resolveCaughtTypes(Type type) {
		List<ITypeBinding> caughtTypes;
		if (type.isUnionType()) //eg. catch(ExceptionA | IOException | CoreException)
		{
			UnionType unionType = (UnionType) type;
			List<Type> types = unionType.types();
			caughtTypes = types.stream().map(Type::resolveBinding).collect(Collectors.toList());
		} else //eg. catch(ExceptionA)
		{
			caughtTypes = Collections.singletonList(type.resolveBinding());
		}
		return caughtTypes;
	}

}
