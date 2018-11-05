package Environment;

public class NotMixedException extends Exception {

	public NotMixedException() {
		super();
	}

	public NotMixedException(String message) {
		super(message);
	}

	public NotMixedException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotMixedException(Throwable cause) {
		super(cause);
	}

	public NotMixedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
