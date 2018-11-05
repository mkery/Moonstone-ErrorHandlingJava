package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class UnavailableException extends Exception {

	private Food food;
	
	public UnavailableException(Food food) {
		this.food = food;
	}

	public Food getUnavailableFood() {
		return food;
	}
}
