package Environment;

public class Airplane {

	public int locX() {
		return 0;
	}

	public int locY() {
		return 0;
	}

	public static int landPlane(Airplane plane, int x, int y, int windSpeed) {
		return 0;
	}

	public Location getLocation() {
		return new Location();
	}

	public static void simulateCrash(Object location, int windSpeed) throws ExplosionException, DisplayException {
		/* some code */
	}

	public class Location {

		public int offset(int targetX, int targetY) {
			return 0;
		}

	}

	public void redraw() {
		/* some code */
	}

	public boolean isArmed() {
		return true;
	}

	public static void simulateGearUp(Airplane plane) throws DisplayException {
		/* some code */
	}

}
