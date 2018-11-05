package Environment;

public class ContentException extends EmailException {

	public ContentException() {
		super();
	}

	public ContentException(String message) {
		super(message);
	}

	public ContentException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContentException(Throwable cause) {
		super(cause);
	}

	public ContentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
