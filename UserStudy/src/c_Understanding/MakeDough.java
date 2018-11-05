package c_Understanding;

import Environment.*;

/**
 * You are implementing code that creates pizza dough. The chat forum you copied
 * it from complained that this code is not really reliable in production.
 * Sometimes this method encounters objects in an incorrect state.
 *
 * Review the code and add “// SKIPPED” comments to lines that might be skipped
 * if an error occurs and may thereby be responsible for problems.
 */

public class MakeDough {

	Dough makeDough() throws NotMixedException {

		// Place comments like so:
		Example.skippedLine(); // SKIPPED

		Pantry pantry = Kitchen.getSharedPantry();
		Tap tap = Kitchen.getSharedTap();

		Food flour = pantry.get("Flour");
		Water water = tap.getWater();
		Food olives = pantry.get("Olives");

		DoughBowl bowl = new DoughBowl();
		try {
			bowl.add(flour);
			bowl.add(water);
			bowl.add(olives);
			bowl.mix();
		} catch (OverflowException e) {
			Kitchen.cleanupCounter();
		}

		return bowl.getDough();
	}

}
