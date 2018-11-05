package Environment;

public class Kitchen {

	private static final Pantry pantry = new Pantry();
	private static final Tap tap = new Tap();
	private static final Refrigerator refrigerator = new Refrigerator();
	private static final Oven oven = new Oven();

	public static Pantry getSharedPantry() {
		return pantry;
	}

	public static Tap getSharedTap() {
		return tap;
	}

	public static Refrigerator getSharedFridge() {
		return refrigerator;
	}

	public static Oven getSharedOven() {
		return oven;
	}

	public static void cleanupCounter() {
	}

}
