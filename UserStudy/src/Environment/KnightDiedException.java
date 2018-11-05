package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class KnightDiedException extends Exception {
	public KnightDiedException() {
		super();
	}

	public KnightDiedException(String message) {
		super(message);
	}

	public KnightDiedException(String message, Throwable cause) {
		super(message, cause);
	}

	public KnightDiedException(Throwable cause) {
		super(cause);
	}

	public KnightDiedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
