package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class OpenedRefrigerator implements AutoCloseable {
	OpenedRefrigerator() {
		/* some code */
	}

	public Food get(String kind) {
		return new Food() {
			@Override
			public String toString() {
				return kind;
			}
		};
	}

	@Override
	public void close() {
		/* some code */
	}
}
