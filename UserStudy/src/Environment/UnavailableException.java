package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class UnavailableException extends Exception {

	private Food food;

	public UnavailableException(Food food) {
		this.food = food;
	}

	public UnavailableException(String message, Food food) {
		super(message);
		this.food = food;
	}

	public UnavailableException(String message, Throwable cause, Food food) {
		super(message, cause);
		this.food = food;
	}

	public UnavailableException(Throwable cause, Food food) {
		super(cause);
		this.food = food;
	}

	public UnavailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Food food) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.food = food;
	}

	public Food getUnavailableFood() {
		return food;
	}
}
