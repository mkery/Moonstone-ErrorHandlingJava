package Environment;

public class DisplayException extends Exception {

	public DisplayException() {
		super();
	}

	public DisplayException(String message) {
		super(message);
	}

	public DisplayException(String message, Throwable cause) {
		super(message, cause);
	}

	public DisplayException(Throwable cause) {
		super(cause);
	}

	public DisplayException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}