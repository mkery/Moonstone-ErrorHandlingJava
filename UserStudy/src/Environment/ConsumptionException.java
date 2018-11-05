package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class ConsumptionException extends Exception {
	public ConsumptionException() {
		super();
	}

	public ConsumptionException(String message) {
		super(message);
	}

	public ConsumptionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConsumptionException(Throwable cause) {
		super(cause);
	}

	public ConsumptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
