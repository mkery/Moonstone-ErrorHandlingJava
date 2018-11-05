package Environment;

public class ServerFailedException extends Exception {

	public ServerFailedException(Throwable cause) {
		/* some code */
	}

	public ServerFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ServerFailedException() {
		super();
	}

	public ServerFailedException(String message) {
		super(message);
	}

	public ServerFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
