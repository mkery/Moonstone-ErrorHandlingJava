package Environment;

public class PlayerException extends Exception {

	public PlayerException() {
		super();
	}

	public PlayerException(String message) {
		super(message);
	}

	public PlayerException(String message, Throwable cause) {
		super(message, cause);
	}

	public PlayerException(Throwable cause) {
		super(cause);
	}

	public PlayerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}