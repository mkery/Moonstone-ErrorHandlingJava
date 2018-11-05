package recommend;

import edu.cmu.moonstone.view.CatchExample;
import moonstone.StartupUtility;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class RecDatabase
{	
	private static final boolean debug = true;

	private static final Map<String, RecommendationRepository> recommendationsPerProject = Collections.synchronizedMap(new HashMap<>());
	private final IProject project;
	private final List<IResource> sourcePaths;

	public RecDatabase(IProject project, List<IResource> sourcePaths)
	{
		this.project = project;
		this.sourcePaths = sourcePaths;
		String projectName = project.getName();
		System.out.println("[recommend] project path "+sourcePaths);
		System.out.println("[recommend] project name "+projectName);
	}

	private Job crawlJob;
	public synchronized void setupDatabase() {
		int delay = 200;
		if (crawlJob != null) {
			int state = crawlJob.getState();
			if (state == Job.WAITING) return;
			if (state != Job.NONE) delay = 0;
			crawlJob.cancel();
		}
		crawlJob = Job.create("Crawl files for eh", monitor -> {
			return crawlNewProject(project, sourcePaths, monitor);
		});
		crawlJob.setPriority(Job.DECORATE);
		crawlJob.setRule(project);
		crawlJob.schedule(delay);
	}
	
	/*
	 * First stage of extracting catch blocks. We search the whole project for java files that contain
	 * the word "try", that are also not ostensibly test files. We consider test files those that contain
	 * "test" in their name. (clearly imperfect). We use a shell script to do this, because... it seems faster
	 * than doing it in java???
	 */
	private static IStatus crawlNewProject(IProject project, List<IResource> sourcePaths, IProgressMonitor monitor)
	{
		if(debug) System.out.println("[recommend] crawling new project for try catch finally");
		//because we're going to wait for a bash command, etc, we want this on a different thread

		RecommendationRepository recommendationRepository = new RecommendationRepository();
		for (IResource sourcePath : sourcePaths) {
			try {
				scrapeFiles(recommendationRepository, sourcePath, monitor);
			} catch (OperationCanceledException e) {
				return Status.CANCEL_STATUS;
			} catch (Throwable t) {
				System.err.println("Crawl failed");
				t.printStackTrace();
				continue;
			}
		}
		recommendationsPerProject.put(project.getName(), new RecommendationRepository(recommendationRepository));
		return Status.OK_STATUS;
	}
	
	/*
	 * Now that we have a list of files that contain "try", keep in mind that these are files
	 * from the user's active project (though many of the files may be closed/currently inactive).
	 */
	private static void scrapeFiles(RecommendationRepository recommendationRepository, IResource container, IProgressMonitor monitor)
	{
		try {
			container.accept(resource -> {
				if (resource.getType() == IResource.FILE && "java".equalsIgnoreCase(resource.getFileExtension())) {
					if (monitor.isCanceled()) throw new OperationCanceledException();
					IFile file = (IFile) resource;

					ICompilationUnit cu = (ICompilationUnit) JavaCore.create(file);
					RecParser.extractEH(recommendationRepository, file.getProjectRelativePath().toString(), cu);
					return false;
				}
				return true;
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static String createCanonicalCode(ExceptionHandler handler) {
		return String.join("\n", handler.getStatements());
	}

	public static void addHandlerToDB(RecommendationRepository recommendationRepository, String fileName, CatchHandler handler) {
		RelevantHandler relevantHandler = createRelevantHandler(fileName, handler);
		for (String exceptionType : handler.getExceptionTypes()) {
			String canonicalCode = createCanonicalCode(handler);
			Map<String, List<RelevantHandler>> handlerGroups = recommendationRepository.catchRecommendations
					.computeIfAbsent(exceptionType, s -> new HashMap<>());
			List<RelevantHandler> handlers = handlerGroups
					.computeIfAbsent(canonicalCode, s -> new ArrayList<>());
			handlers.add(relevantHandler);
		}
	}

	public static void addHandlerToDB(RecommendationRepository recommendationRepository, String fileName, FinallyHandler handler) {
		RelevantHandler relevantHandler = createRelevantHandler(fileName, handler);
		String canonicalCode = createCanonicalCode(handler);
		List<RelevantHandler> handlers = recommendationRepository.finallyRecommendations
				.computeIfAbsent(canonicalCode, s -> new ArrayList<>());
		handlers.add(relevantHandler);
	}

	private static RelevantHandler createRelevantHandler(String fileName, ExceptionHandler handler) {
		String typeName = "";
		String methodName = "";
		for (ASTNode parent = handler.getNode().getParent(); parent != null; parent = parent.getParent()) {
			if (parent.getNodeType() == ASTNode.METHOD_DECLARATION)
			{
				MethodDeclaration method = (MethodDeclaration) parent;
				methodName = method.getName().getIdentifier() + "()";
			}
			if (parent.getNodeType() == ASTNode.TYPE_DECLARATION)
			{
				TypeDeclaration type = (TypeDeclaration) parent;
				typeName = type.getName().getIdentifier();
				break;
			}
		}
		return new RelevantHandler(fileName, typeName, methodName, handler.getNode());
	}

	/*
	 * retrieve matches
	 */
	public static List<CatchExample[]> retrieveMatches(List<String> exceptionTypes, IProject project, IFile ignoreFile, Position ignorePosition)
	{
		//first test, look for Ada.java_c_1533
		String projectName = project.getName();

		RecommendationRepository recommendationRepository = recommendationsPerProject.get(projectName);
		if (recommendationRepository == null) return Collections.emptyList();

		Map<String, List<CatchExample>> handlerGroups = new HashMap<>();
		for (String exceptionType : exceptionTypes) {
			Map<String, List<RelevantHandler>> handlerGroupsForExceptionType =
					recommendationRepository.catchRecommendations.getOrDefault(exceptionType, null);
			if (handlerGroupsForExceptionType == null) continue;

			createExamples(handlerGroupsForExceptionType, handlerGroups, exceptionType, ignoreFile, ignorePosition);
		}

		return handlerGroups.values().stream()
				.filter(catchExamples -> !catchExamples.isEmpty())
				.map(catchExamples -> catchExamples.toArray(new CatchExample[catchExamples.size()]))
				.collect(Collectors.toList());
	}

	/*
	 * retrieve matches for finally block
	 */
	public static List<CatchExample[]> retrieveMatches(IProject project, IFile ignoreFile, Position ignorePosition)
	{
		//first test, look for Ada.java_c_1533
		String projectName = project.getName();

		RecommendationRepository recommendationRepository = recommendationsPerProject.get(projectName);
		if (recommendationRepository == null) return Collections.emptyList();

		Map<String, List<CatchExample>> handlerGroups = new HashMap<>();
		createExamples(recommendationRepository.finallyRecommendations, handlerGroups,
				"finally", ignoreFile, ignorePosition);

		return handlerGroups.values().stream()
				.filter(catchExamples -> !catchExamples.isEmpty())
				.map(catchExamples -> catchExamples.toArray(new CatchExample[catchExamples.size()]))
				.collect(Collectors.toList());
	}

	private static boolean notCurrentPosition(RelevantHandler handler, IFile file, Position position) {
		String fileName = file.getProjectRelativePath().toString();
		return !handler.getFile().equals(fileName)
				|| handler.getCode().getStartPosition() + handler.getCode().getLength() <= position.getOffset()
				|| handler.getCode().getStartPosition() > position.getOffset() + position.getLength();
	}

	private static void createExamples(Map<String, List<RelevantHandler>> input, Map<String, List<CatchExample>> output, String exceptionType, IFile ignoreFile, Position ignorePosition) {
		input.forEach((canonicalCode, relevantHandlers) -> {
			List<CatchExample> catchExampleList = output.computeIfAbsent(canonicalCode, s -> new ArrayList<>());
			relevantHandlers.stream()
					.filter(handler -> notCurrentPosition(handler, ignoreFile, ignorePosition))
					.collect(Collectors.groupingBy(RelevantHandler::getType))
					.values().stream()
					.map(handlers -> {
						RelevantHandler relevantHandler = handlers.get(0);
						List<String> methods = handlers.stream().map(RelevantHandler::getMethod).collect(Collectors.toList());
						List<ASTNode> nodes = handlers.stream().map(RelevantHandler::getCode).collect(Collectors.toList());
						return new CatchExample(relevantHandler.getFile(), relevantHandler.getType(), exceptionType, methods, nodes);
					})
					.forEach(catchExampleList::add);
		});
	}

	public void addASTListener(IPartService partService, IEditorPart editorPart) {
		/*
		 * (we're still in earlyStartup method)
		 * Adds a listener to Eclipse's JDT, so we can get the most updated version of the Java AST
		 */
		IElementChangedListener elementChangedListener = ev -> {
			if (ev.getDelta().getCompilationUnitAST() != null) setupDatabase();
		};
		JavaCore.addElementChangedListener(elementChangedListener);

		partService.addPartListener(new StartupUtility.IPartDeactivatedListener() {
			@Override
			public void partDeactivated(IWorkbenchPart part) {
				if (part == editorPart) {
					JavaCore.removeElementChangedListener(elementChangedListener);
					partService.removePartListener(this);
				}
			}
		});
	}
}
