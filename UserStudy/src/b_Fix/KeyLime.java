package b_Fix;

import Environment.*;
import Environment.PantryDrawer.Session;

/**
 * Your team has to implement code that makes a key lime pie, and you start from
 * this code found on the Internet. You're skeptical whether it behaves
 * correctly, if something goes wrong.
 *
 * Please check this code and make changes where needed to ensure that it avoids
 * bad exception handling practices and behaves correctly in face of exceptions.
 */

public class KeyLime {

	public Food makeKeyLimePie() throws Exception {
		Session pantrySession;

		PantryDrawer sharedDrawer = Kitchen.getSharedPantryDrawer();
		if (!sharedDrawer.isLocked()) {
			pantrySession = sharedDrawer.startSession();
		} else {
			PantryDrawer privateDrawer = Kitchen.getPrivatePantryDrawer("Roomee");
			pantrySession = privateDrawer.startSession();
		}

		Food flour = pantrySession.get("Flour");
		Food limes = pantrySession.get("Limes");
		if (flour == null || limes == null)
			throw new PieException("Missing ingredient(s)");

		pantrySession.close();

		Dough dough = new Dough(flour);
		Food[] ingredients = {dough, limes};
		return new Pie(ingredients);
	}
}
