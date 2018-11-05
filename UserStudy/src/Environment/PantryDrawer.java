package Environment;

public class PantryDrawer {

	public class Session implements AutoCloseable {
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

	public Session startSession() {
		return new Session();
	}

	public boolean isLocked() {
		return false;
	}

}
