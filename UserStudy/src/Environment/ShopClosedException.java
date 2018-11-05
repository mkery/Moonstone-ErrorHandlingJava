package Environment;

public class ShopClosedException extends Exception {

	public ShopClosedException() {
		super();
	}

	public ShopClosedException(String message) {
		super(message);
	}

	public ShopClosedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ShopClosedException(Throwable cause) {
		super(cause);
	}

	public ShopClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
