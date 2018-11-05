package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class PizzaBurntException extends Exception {
	public PizzaBurntException() {
		super();
	}

	public PizzaBurntException(String message) {
		super(message);
	}

	public PizzaBurntException(String message, Throwable cause) {
		super(message, cause);
	}

	public PizzaBurntException(Throwable cause) {
		super(cause);
	}

	public PizzaBurntException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
