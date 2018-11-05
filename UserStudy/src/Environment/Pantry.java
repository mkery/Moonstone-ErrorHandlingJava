package Environment;

public class Pantry {

	public Food get(String kind) {
		return new Food() {
			@Override
			public String toString() {
				return kind;
			}
		};
	}

}
