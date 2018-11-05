package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class OverflowException extends Exception {
	public OverflowException() {
		super();
	}

	public OverflowException(String message) {
		super(message);
	}

	public OverflowException(String message, Throwable cause) {
		super(message, cause);
	}

	public OverflowException(Throwable cause) {
		super(cause);
	}

	public OverflowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
