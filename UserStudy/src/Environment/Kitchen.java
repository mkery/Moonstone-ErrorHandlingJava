package Environment;

public class Kitchen {

	private static final Pantry pantry = new Pantry();
	private static final PantryDrawer pantryDrawer = new PantryDrawer();
	private static final Tap tap = new Tap();
	private static final Oven oven = new Oven();

	public static Pantry getSharedPantry() {
		return pantry;
	}

	public static PantryDrawer getSharedPantryDrawer() {
		return pantryDrawer;
	}

	public static Tap getSharedTap() {
		return tap;
	}

	public static OpenedRefrigerator openFridge() {
		return new OpenedRefrigerator();
	}

	public static Oven getSharedOven() {
		return oven;
	}

	public static void cleanupCounter() {
		/* some code */
	}

	public static PantryDrawer getPrivatePantryDrawer(String person) {
		return new PantryDrawer();
	}

}
