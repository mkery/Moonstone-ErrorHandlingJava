package moonstone;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.markers.MyMarkerFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;

import java.util.*;
import java.util.stream.Stream;

/***
 * A class for managing and reporting problems we identify with their exception handling code
 * Work-in-progress
 */
public class ProblemsInErrorHandler 
{
	private final static boolean debug = false; // flag to include debug print statements
	private final ICompilationUnit cu;

	private MyMarkerFactory markerFactory;

	public ProblemsInErrorHandler(ICompilationUnit cu) {
		this.cu = cu;
		markerFactory = new MyMarkerFactory();
	}

	public void diagnose(ICompilationUnit cu)
	{
		CompilationUnit ast = EditorUtility.parseAST(cu);
		if (ast != null) diagnose(ast, new NullProgressMonitor());
	}

	/*
	 * for diagnosis on a specific AST node
	 */
	public IStatus diagnose(CompilationUnit ast, IProgressMonitor monitor) {
		ICompilationUnit cu = (ICompilationUnit) ast.getJavaElement();
		if(debug) System.out.println("[ProblemsInErrors] Diagnosing AST");
		ExceptionVisitor visitor = new ExceptionVisitor(this, monitor);
		IResource resource = cu.getResource();
		markerFactory.beginUpdate(resource, ast);
		try {
			//Use the Exception Visitor to walk this AST looking for issues
			visitor.process(resource, ast);
			return Status.OK_STATUS;
		} catch (OperationCanceledException e) {
			markerFactory.cancelUpdate(resource);
			return Status.CANCEL_STATUS;
		} finally {
			markerFactory.endUpdate(resource);
		}
	}


	private <T> Pair<Optional<T>,List<T>> addEntry(TreeMap<Integer, T> entries, T value, int start, int length) {
		synchronized (entries) {
			ArrayList<T> subRanges = new ArrayList<>(0);
			if (value == null || length < 2) return Pair.of(Optional.empty(), subRanges);

			// Super range
			Map.Entry<Integer, T> entry = entries.floorEntry(start);
			if (entry != null && entry.getValue() != null) return Pair.of(Optional.of(entry.getValue()), subRanges);

			// Sub ranges
			NavigableMap<Integer, T> covered = entries.subMap(start, false, start + length, false);
			subRanges.ensureCapacity(covered.values().size() / 2);
			covered.values().stream().filter(Objects::nonNull).forEach(subRanges::add);
			covered.clear();

			entries.put(start, value);
			entries.put(start + length - 1, null);
			return Pair.of(Optional.empty(), subRanges);
		}
	}

	private <T> void removeEntry(TreeMap<Integer, T> entries, T value, int start, int length) {
		synchronized (entries) {
			entries.remove(start, value);
			entries.remove(start + length - 1, null);
		}
	}

	/*
	 * Called by the AST Exception walker to report back issues
	 */
	public void report(Problem problem, Region pos, IResource resource)
	{
		try{
			switch(problem)
			{
			case CATCH_EXCEPTION:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Catching Exception", MarkerDef.CAUGHT_EXCEPTION_MARKER);
				break;
			case CATCH_ERROR:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Catching Error", MarkerDef.CAUGHT_ERROR_MARKER);
				break;	
			case CATCH_RUNTIMEEXCEPTION:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Catching RuntimeException", MarkerDef.CAUGHT_RUNTIME_MARKER);
				break;
			case CATCH_THROWABLE:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Catching Throwable", MarkerDef.CAUGHT_THROWABLE_MARKER);
				break; 
			case THROWS_EXCEPTION:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Declares Exception", MarkerDef.THROWS_EXCEPTION_MARKER);
				break;
			case THROWS_THROWABLE:
				if(debug) System.out.println("[ProblemsInErrors] Adding problem marker at "+pos);
				markerFactory.createMarker(resource, pos, "Declares Throwable", MarkerDef.THROWS_THROWABLE_MARKER);
				break;
			case EMPTY_CATCH:
				markerFactory.createMarker(resource, pos, "Swallowing Exception", MarkerDef.EMPTY_CATCH_MARKER);
				break; 
			case THROWFROMTRY_ERROR:
				markerFactory.createMarker(resource, pos, "Throwing Error", MarkerDef.THREWFROMTRY_ERROR_MARKER);
				break;
			case THROWFROMTRY_EXCEPTION:
				markerFactory.createMarker(resource, pos, "Throwing Exception", MarkerDef.THREWFROMTRY_EXCEPTION_MARKER);
				break;
			case THROWFROMTRY_RUNTIMEEXCEPTION:
				markerFactory.createMarker(resource, pos, "Throwing RuntimeException", MarkerDef.THREWFROMTRY_RUNTIME_MARKER);
				break;
			case THROWFROMTRY_THROWABLE:
				markerFactory.createMarker(resource, pos, "Throwing Throwable", MarkerDef.THREWFROMTRY_THROWABLE_MARKER);
				break;
			case THROWFROMCATCH_ERROR:
				markerFactory.createMarker(resource, pos, "Throwing Error", MarkerDef.THREWFROMCATCH_ERROR_MARKER);
				break;
			case THROWFROMCATCH_EXCEPTION:
				markerFactory.createMarker(resource, pos, "Throwing Exception", MarkerDef.THREWFROMCATCH_EXCEPTION_MARKER);
				break;
			case THROWFROMCATCH_RUNTIMEEXCEPTION:
				markerFactory.createMarker(resource, pos, "Throwing RuntimeException", MarkerDef.THREWFROMCATCH_RUNTIME_MARKER);
				break;
			case THROWFROMCATCH_THROWABLE:
				markerFactory.createMarker(resource, pos, "Throwing Throwable", MarkerDef.THREWFROMCATCH_THROWABLE_MARKER);
				break;
			case DESTRUCTIVE_WRAP:
				markerFactory.createMarker(resource, pos, "Destructively Wrapping", MarkerDef.DESTRUCTIVE_WRAP_MARKER);
				break;
			case RESOURCE_LEAK:
				markerFactory.createMarker(resource, pos, "Resource Leak", MarkerDef.RESOURCE_LEAK_MARKER);
				break;
			}
		}
		catch(CoreException e)
		{
			//TODO exception handling
			 //logger.log(Level.WARNING, e.getStackTrace()+ "failure in report");
			 e.printStackTrace();
		}
	}
	
	private class DiagnoseJob extends Job {
		private final CompilationUnit ast;

		DiagnoseJob(CompilationUnit ast) {
			super("Searching for exception handling problems");
			if (ast == null) throw new IllegalArgumentException();
			this.ast = ast;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				return diagnose(ast, monitor);
			} catch (Throwable t) {
				System.err.println("Searching for exception handling problems failed");
				t.printStackTrace();
				return Status.OK_STATUS;
			} finally {
				monitor.done();
			}
		}
    }

	private Map<ICompilationUnit, DiagnoseJob> diagnoseJobs = new HashMap<>();
	private synchronized void scheduleDiagnosis(CompilationUnit ast) {
		DiagnoseJob diagnoseJob = new DiagnoseJob(ast);
		DiagnoseJob prev = diagnoseJobs.replace(cu, diagnoseJob);
		int delay = 200;
		if (prev != null) {
			int state = prev.getState();
			if (state == Job.WAITING || prev.ast == ast) return;
			if (state != Job.NONE) delay = 0;
			prev.cancel();
		}
		diagnoseJob.setPriority(Job.DECORATE);
		diagnoseJob.setRule(cu.getSchedulingRule());
		diagnoseJob.schedule(delay);
	}

	void addASTListener(IPartService partService, IEditorPart editorPart) {
		/*
		 * (we're still in earlyStartup method)
		 * Adds a listener to Eclipse's JDT, so we can get the most updated version of the Java AST
		 */
		IElementChangedListener elementChangedListener = event -> {
			CompilationUnit ast = event.getDelta().getCompilationUnitAST();
			if (ast != null && cu.equals(ast.getJavaElement())) scheduleDiagnosis(ast);
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
