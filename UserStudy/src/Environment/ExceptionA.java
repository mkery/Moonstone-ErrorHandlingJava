package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class ExceptionA extends Exception {
	public ExceptionA() {
		/* some code */
	}

	public ExceptionA(String message) {
		super(message);
	}

	public ExceptionA(String message, Throwable cause) {
		super(message, cause);
	}

	public ExceptionA(Throwable cause) {
		super(cause);
	}

	public ExceptionA(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
