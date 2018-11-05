package moonstone;

import edu.cmu.moonstone.ghostcomment.GhostCommentPainter;
import edu.cmu.moonstone.quickfix.MarkerManager;
import edu.cmu.moonstone.recommend.RecManager;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.*;
import recommend.RecDatabase;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*
 * [mb] 
 * On startup, this class sets up document listeners to notify the plug-in of any text changes
 * the user makes. 
 * 
 */
public class StartupUtility implements IStartup
{
	private static final boolean debug = false; //flag, when turned on, allows extra print statements on the status of the plug-in

	//region Listener Interfaces

	public interface IWindowOpenedListener extends IWindowListener {
		@Override
		default void windowActivated(IWorkbenchWindow window) {}

		@Override
		default void windowDeactivated(IWorkbenchWindow window) {}

		@Override
		default void windowClosed(IWorkbenchWindow window) {}
	}

	public interface IPageOpenedListener extends IPageListener {
		@Override
		default void pageActivated(IWorkbenchPage page) {}

		@Override
		default void pageClosed(IWorkbenchPage page) {}
	}

	public interface IPartListener extends org.eclipse.ui.IPartListener {
		@Override
		default void partActivated(IWorkbenchPart part) {}

		@Override
		default void partBroughtToTop(IWorkbenchPart part) {}

		@Override
		default void partClosed(IWorkbenchPart part) {}

		@Override
		default void partDeactivated(IWorkbenchPart part) {}

		@Override
		default void partOpened(IWorkbenchPart part) {}
	}

	public interface IPartDeactivatedListener extends org.eclipse.ui.IPartListener {
		@Override
		default void partActivated(IWorkbenchPart part) {}

		@Override
		default void partBroughtToTop(IWorkbenchPart part) {}

		@Override
		default void partClosed(IWorkbenchPart part) {}

		@Override
		default void partOpened(IWorkbenchPart part) {}
	}

	//endregion

	/**
	 * earlyStartup runs as the plug-in is starting... so avoid putting too much computation here
	 */
	@Override
	public void earlyStartup() {
		/*new DeadlockDetector(deadlockedThreads -> {
			if (deadlockedThreads == null) return;
			System.err.println("Deadlock detected!");

			printDeadlockedThreads(deadlockedThreads, System.err);
		}, 15, TimeUnit.SECONDS).start();*/

		//all workbenches
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> initializeWorkbench(PlatformUI.getWorkbench()));
	}

	/*private void printDeadlockedThreads(ThreadInfo[] deadlockedThreads, PrintStream stream) {
		for (ThreadInfo ti : deadlockedThreads) {
			if (ti == null) continue;

			// Thread
            StringBuilder sb = new StringBuilder("\"").append(ti.getThreadName()).append("\"")
                    .append(" Id=").append(ti.getThreadId())
                    .append(" in ").append(ti.getThreadState());
            if (ti.getLockName() != null) sb.append(" on lock=").append(ti.getLockName());
            if (ti.isSuspended()) sb.append(" (suspended)");
            if (ti.isInNative()) sb.append(" (running in native)");
            stream.println(sb);
            if (ti.getLockOwnerName() != null)
				stream.println("\towned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());

            // StackTrace with Monitors
            StackTraceElement[] stackTrace = ti.getStackTrace();
            MonitorInfo[] monitors = ti.getLockedMonitors();
            for (int i = 0; i < stackTrace.length; i++) {
                stream.println("\tat " + stackTrace[i]);
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == i) stream.println("\t  - locked " + mi);
                }
            }

            // Synchronizers
            LockInfo[] locks = ti.getLockedSynchronizers();
            stream.println("\tLocked synchronizers: count = " + locks.length);
            for (LockInfo li : locks) stream.println("\t  - " + li);

            stream.println();
        }
	}*/

	private void initializeWorkbench(IWorkbench workbench) {
		Hashtable<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		//get initial startup window and add listeners
		Arrays.asList(workbench.getWorkbenchWindows()).forEach(this::initializeWindow);

		/*
		 * for all future windows that get added, add appropriate listeners so we are
		 * notified of text edits to java code
		 */
		workbench.addWindowListener((IWindowOpenedListener) this::initializeWindow);
	}

	private void initializeWindow(IWorkbenchWindow window) {
		if(debug) System.out.println("[StartupUtility] Window opened");
		for (IWorkbenchPage page : window.getPages()) {
			initializePage(page);
		}
		window.addPageListener((IPageOpenedListener) this::initializePage);
	}

	/**
	 * Make sure we can listen to text changes on any page that is added or activated in the future
	 */
	private void initializePage(IWorkbenchPage page) {
		//open editors
		for (IEditorReference editorReference : page.getEditorReferences()) {
			IEditorPart editor = editorReference.getEditor(true);
			if (editor != null) initializeEditor(editor);
		}
		prepareEditor(page, page.getActiveEditor());

		page.addPartListener(new IPartListener() {
			@Override
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					IEditorPart editorPart = (IEditorPart) part;
					prepareEditor(page, editorPart);
				}
			}

			@Override
			public void partOpened(IWorkbenchPart part) {
				if (part instanceof IEditorPart) {
					IEditorPart editorPart = (IEditorPart) part;
					initializeEditor(editorPart);
				}
			}
		});
	}

	/**
	 * For each page, make sure we can listen to the IDocument, and the actual text edits
	 */
	private void initializeEditor(IEditorPart editorPart) {
		if (!(editorPart.getAdapter(ITextOperationTarget.class) instanceof ITextViewer)) return;

		GhostCommentPainter ghostCommentPainter = new GhostCommentPainter(editorPart);
		ghostCommentPainter.addToEditor();

		/*
		 * Adding manager for markers
		 */
		MarkerManager markerManager = new MarkerManager(editorPart);
		//TODO: Add the click listener to the column, and the hover listener to the StyledText
		markerManager.addMarkerListener();

		/*
		 * Adding manager for Recommendations
		 */
		RecManager recManager = new RecManager(editorPart);
		//Add the hover listener
		recManager.addStyledTextListener();
	}

	private void prepareEditor(IWorkbenchPage page, IEditorPart editorPart) {
		//Get this editor's text input (user's program), document, file information
		IEditorInput input = editorPart.getEditorInput();
		if (!(input instanceof IFileEditorInput)) return;

		IFileEditorInput filer = (IFileEditorInput) input;
		IProject activeProject = filer.getFile().getProject();

		//get location of current project
		if (activeProject != null) {
			try {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				IJavaProject javaProject = JavaCore.create(activeProject);
				IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
				List<IResource> paths = Arrays.stream(resolvedClasspath)
						.filter(classpathEntry -> classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE)
						.map(IClasspathEntry::getPath)
						.map(root::findMember)
						.collect(Collectors.toList());

				RecDatabase recDatabase = new RecDatabase(activeProject, paths);
				recDatabase.setupDatabase();
				recDatabase.addASTListener(page, editorPart);
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}

		//scan for any try blocks in the currently opening Page
		IJavaElement inputJavaElement = JavaUI.getEditorInputJavaElement(input);
		if (!(inputJavaElement instanceof ICompilationUnit)) return;

		ICompilationUnit cu = (ICompilationUnit) inputJavaElement;
		ProblemsInErrorHandler problemsInErrorHandler = new ProblemsInErrorHandler(cu);
		problemsInErrorHandler.diagnose(cu);
		problemsInErrorHandler.addASTListener(page, editorPart);
	}

	public static <T> Supplier<T> lazily(Callable<T> factory) {
		FutureTask<T> task = new FutureTask<>(factory);
		return () -> {
			task.run();
			try {
				return task.get();
			} catch (InterruptedException e) {
				// Restore the interrupted status
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			} catch (ExecutionException e) {
				try {
					throw e.getCause();
				} catch (RuntimeException | Error t) {
					throw t;
				} catch (Throwable t) {
					// Factory should not be able to throw checked exceptions
					throw new AssertionError(t);
				}
			}
		};
	}
}
