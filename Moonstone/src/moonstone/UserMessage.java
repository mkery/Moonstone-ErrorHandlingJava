package moonstone;

import org.eclipse.osgi.util.NLS;

import edu.cmu.moonstone.markers.MarkerDef;
import edu.cmu.moonstone.quickfix.Description;

/**
 * Class that simply holds the scripts for messages we display to users
 */
public class UserMessage extends NLS {
	private static final String BUNDLE_NAME = "moonstone.messages"; //$NON-NLS-1$

    /**
     * The problem with catching Exception is that if the method you are calling later adds
     * a new checked exception to its method signature, the developer's intent is that you
     * should handle the specific new exception. If your code just catches Exception (or Throwable),
     * you'll never know about the change and the fact that your code is now wrong and might break
     * at any point of time in runtime.
     */
    public static String EXCEPTION;
    public static String EXCEPTION_HEADER;
    public static String THROWS_EXCEPTION;
    public static String THROWS_EXCEPTION_HEADER;

    /**
     * Throwable is the superclass of all errors and exceptions in Java.
     * Error is the superclass of all errors which are not meant to be caught by applications.
     * Thus, catching Throwable would essentially mean that Errors such as system exceptions
     * (e.g., OutOfMemoryError, StackOverFlowError or InternalError) would also get caught.
     * And, the recommended approach is that application should not try and recover from Errors
     * such as these. Thus, Throwable and Error classes should not be caught. Only Exception
     * and its subclasses should be caught.
     */
    public static String ERROR;
    public static String ERROR_HEADER;

    //The recommended approach is that application should not try and recover from Errors such as these. 

    /**
     * Java errors are also subclasses of the Throwable.
     * Errors are irreversible conditions that can not be handled by JVM itself. And for some JVM
     * implementations, JVM might not actually even invoke your catch clause on an Error.
     */
    public static String THROWABLE;
    public static String THROWABLE_HEADER;
    public static String THROWS_THROWABLE;
    public static String THROWS_THROWABLE_HEADER;

    /**
     * For RunTimeException (unchecked exceptions) it is said that we should not catch them as they indicate
     * application code errors. If we are catching Exception class directly we also catch RuntimeException as
     * RuntimeException class inherits from Exception.
     */
    public static String RUNTIMEEXCEPTION;
    public static String RUNTIMEEXCEPTION_HEADER;

    /**
     * Nothing is more worse than empty catch block, because it not just hides the Errors and Exception,
     * but also may leave your object in unusable or corrupt state. Empty catch block only make sense,
     * if you absolutely sure that Exception is not going to affect object state on any ways, but still its
     * better to log any error comes during program execution. This is not a Java best practice, but a most
     * common practice, while writing Exception handling code in Java.
     */
    public static String EMPTY_CATCH;
    public static String EMPTY_CATCH_HEADER;

    /**
     * The second exception will come out of method and the original first exception (correct reason) will be lost forever.
     * If the code that you call in a finally block can possibly throw an exception, make sure that you either handle it,
     * or log it. Never let it come out of the finally block.
     */
    public static String THROW_ERROR;
    public static String THROW_ERROR_HEADER;


    /**
     * It simply defeats the whole purpose of having checked exception. Declare the specific checked
     * exceptions that your method can throw. If there are just too many such checked exceptions,
     * you should probably wrap them in your own exception and add information to in exception message.
     * You can also consider code refactoring also if possible.
     */
    public static String THROW_EXCEPTION;
    public static String THROW_EXCEPTION_HEADER;

    /**
     * Generic exceptions such as Error, RuntimeException, Throwable and Exception should never be thrown
     * The primary reason why one should avoid throwing Generic Exceptions, Throwable, Error etc is that doing in
     * this way prevents classes from catching the intended exceptions. Thus, a caller cannot examine the exception
     * to determine why it was thrown and consequently cannot attempt recovery.  Additionally, catching RuntimeException
     * is considered as a bad practice. And, thus, throwing Generic Exceptions/Throwable would lead the developer to
     * catch the exception at a later stage which would eventually lead to further code smells.
     */
    public static String THROW_RUNTIMEEXCEPTION;
    public static String THROW_RUNTIMEEXCEPTION_HEADER;


    public static String THROW_THROWABLE;
    public static String THROW_THROWABLE_HEADER;


    public static String DESTRUCTIVE_WRAP;
    public static String DESTRUCTIVE_WRAP_HEADER;


    public static String RESOURCE_LEAK;
    public static String RESOURCE_LEAK_HEADER;


    public static Description createDescription(String origType) {
        switch (origType) {
            case MarkerDef.CAUGHT_EXCEPTION_MARKER:
                return new Description(EXCEPTION, "Catching Exception\n"
                        , EXCEPTION_HEADER
                        , "http://cdn.skim.gs/image/upload/v1456344012/msi/Puppy_2_kbhb4a.jpg"); //(Body, Title, Header, URL)
            case MarkerDef.CAUGHT_ERROR_MARKER:
                return new Description(ERROR, "Catching Error\n"
                        , ERROR_HEADER
                        , "https://upload.wikimedia.org/wikipedia/commons/7/71/St._Bernard_puppy.jpg");
            case MarkerDef.CAUGHT_THROWABLE_MARKER:
                return new Description(THROWABLE, "Catching Throwable\n"
                        , THROWABLE_HEADER
                        , "http://puppytoob.toobnetwork.com/wp-content/uploads/sites/3/2015/05/corgi.jpg");
            case MarkerDef.CAUGHT_RUNTIME_MARKER:
                return new Description(RUNTIMEEXCEPTION, "Catching Runtime Exception\n"
                        , RUNTIMEEXCEPTION_HEADER
                        , "http://www.callmobilevet.com/wp-content/uploads/puppy.jpg");
            case MarkerDef.THROWS_EXCEPTION_MARKER:
                return new Description(THROWS_EXCEPTION, "Declaring to throw Exception\n"
                        , THROWS_EXCEPTION_HEADER
                        , "http://cdn.skim.gs/image/upload/v1456344012/msi/Puppy_2_kbhb4a.jpg"); //(Body, Title, Header, URL)
            case MarkerDef.THROWS_THROWABLE_MARKER:
                return new Description(THROWS_THROWABLE, "Declaring to throw Throwable\n"
                        , THROWS_THROWABLE_HEADER
                        , "http://puppytoob.toobnetwork.com/wp-content/uploads/sites/3/2015/05/corgi.jpg");
            case MarkerDef.THREWFROMTRY_EXCEPTION_MARKER:
                return new Description(THROW_EXCEPTION, "Throwing Exception\n"
                        , THROW_EXCEPTION_HEADER
                        , "https://pixabay.com/static/uploads/photo/2015/02/05/12/09/chihuahua-624924_960_720.jpg");
            case MarkerDef.THREWFROMTRY_ERROR_MARKER:
                return new Description(THROW_ERROR, "Throwing Error\n"
                        , THROW_ERROR_HEADER
                        , "https://pbs.twimg.com/profile_images/446566229210181632/2IeTff-V.jpeg");
            case MarkerDef.THREWFROMTRY_THROWABLE_MARKER:
                return new Description(THROW_THROWABLE, "Throwing Throwable\n"
                        , THROW_THROWABLE_HEADER
                        , "http://img15.deviantart.net/db08/i/2015/234/3/c/cute_puppy_by_knobiobiwan-d96oqwf.jpg");
            case MarkerDef.THREWFROMTRY_RUNTIME_MARKER:
                return new Description(THROW_RUNTIMEEXCEPTION, "Throwing Runtime Exception\n"
                        , THROW_RUNTIMEEXCEPTION_HEADER
                        , "http://www.pitt.edu/~egs21/infocute.jpg");
            case MarkerDef.THREWFROMCATCH_EXCEPTION_MARKER:
                return new Description(THROW_EXCEPTION, "Throwing Exception\n"
                        , THROW_EXCEPTION_HEADER
                        , "https://pixabay.com/static/uploads/photo/2015/02/05/12/09/chihuahua-624924_960_720.jpg");
            case MarkerDef.THREWFROMCATCH_ERROR_MARKER:
                return new Description(THROW_ERROR, "Throwing Error\n"
                        , THROW_ERROR_HEADER
                        , "https://pbs.twimg.com/profile_images/446566229210181632/2IeTff-V.jpeg");
            case MarkerDef.THREWFROMCATCH_THROWABLE_MARKER:
                return new Description(THROW_THROWABLE, "Throwing Throwable\n"
                        , THROW_THROWABLE_HEADER
                        , "http://img15.deviantart.net/db08/i/2015/234/3/c/cute_puppy_by_knobiobiwan-d96oqwf.jpg");
            case MarkerDef.THREWFROMCATCH_RUNTIME_MARKER:
                return new Description(THROW_RUNTIMEEXCEPTION, "Throwing Runtime Exception\n"
                        , THROW_RUNTIMEEXCEPTION_HEADER
                        , "http://www.pitt.edu/~egs21/infocute.jpg");
            case MarkerDef.DESTRUCTIVE_WRAP_MARKER:
                return new Description(DESTRUCTIVE_WRAP, "Destructive Wrap\n"
                        , DESTRUCTIVE_WRAP_HEADER
                        , "https://pixabay.com/static/uploads/photo/2016/03/05/05/53/puppy-1237213_960_720.jpg");
            case MarkerDef.RESOURCE_LEAK_MARKER:
                return new Description(RESOURCE_LEAK, "Resource Leak\n"
                        , RESOURCE_LEAK_HEADER
                        , "http://www.tehcute.com/pics/201402/1414688188_those-puppy-eyes-big.jpg");
            default:
                return new Description(EMPTY_CATCH, "Empty Catch\n"
                        , EMPTY_CATCH_HEADER
                        , "https://blog.imgur.com/wp-content/uploads/2016/03/puppy.jpg");
        }
    }

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UserMessage.class);
    }

    private UserMessage() {
    }
}
