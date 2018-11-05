package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class VeryNiceException extends Exception {
	public VeryNiceException() {
		super();
	}

	public VeryNiceException(String message) {
		super(message);
	}

	public VeryNiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public VeryNiceException(Throwable cause) {
		super(cause);
	}

	public VeryNiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
