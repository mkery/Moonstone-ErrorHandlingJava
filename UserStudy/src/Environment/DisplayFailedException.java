package Environment;

public class DisplayFailedException extends Exception {

	public DisplayFailedException() {
		super();
	}

	public DisplayFailedException(String message) {
		super(message);
	}

	public DisplayFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DisplayFailedException(Throwable cause) {
		/* some code */
	}

	public DisplayFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
