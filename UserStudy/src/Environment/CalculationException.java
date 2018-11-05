package Environment;

public class CalculationException extends Exception {
	public CalculationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CalculationException(Throwable cause) {
		super(cause);
	}

	public CalculationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CalculationException() {
		super();
	}

	public CalculationException(String message) {
		super(message);
	}
}
