package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class SomeOtherException extends VeryNiceException {
	public SomeOtherException(Throwable cause) {
		/* some code */
	}

	public SomeOtherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SomeOtherException() {
		super();
	}

	public SomeOtherException(String message) {
		super(message);
	}

	public SomeOtherException(String message, Throwable cause) {
		super(message, cause);
	}
}
