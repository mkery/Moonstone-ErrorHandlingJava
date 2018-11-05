package Environment;

public class ExplosionException extends Exception {

	public ExplosionException() {
		super();
	}

	public ExplosionException(String message) {
		super(message);
	}

	public ExplosionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExplosionException(Throwable cause) {
		super(cause);
	}

	public ExplosionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}