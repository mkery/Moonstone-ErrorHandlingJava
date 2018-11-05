package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class TooImpatientException extends Exception {
	public TooImpatientException() {
		super();
	}

	public TooImpatientException(String message) {
		super(message);
	}

	public TooImpatientException(String message, Throwable cause) {
		super(message, cause);
	}

	public TooImpatientException(Throwable cause) {
		super(cause);
	}

	public TooImpatientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
