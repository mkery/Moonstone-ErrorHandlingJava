package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class CounterAttackException extends Exception {

	public CounterAttackException() {
		super();
	}

	public CounterAttackException(String message) {
		super(message);
	}

	public CounterAttackException(String message, Throwable cause) {
		super(message, cause);
	}

	public CounterAttackException(Throwable cause) {
		super(cause);
	}

	public CounterAttackException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
