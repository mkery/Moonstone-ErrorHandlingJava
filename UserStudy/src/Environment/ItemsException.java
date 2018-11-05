package Environment;

public class ItemsException extends PlayerException {

	public ItemsException() {
		super();
	}

	public ItemsException(String message) {
		super(message);
	}

	public ItemsException(String message, Throwable cause) {
		super(message, cause);
	}

	public ItemsException(Throwable cause) {
		super(cause);
	}

	public ItemsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}