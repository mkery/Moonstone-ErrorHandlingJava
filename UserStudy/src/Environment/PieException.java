package Environment;

public class PieException extends Exception {

	public PieException(String string) {
		/* some code */
	}

	public PieException(String message, Throwable cause) {
		super(message, cause);
	}

	public PieException(Throwable cause) {
		super(cause);
	}

	public PieException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PieException() {
		super();
	}
}
