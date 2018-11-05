package b_Fix;

import java.util.ArrayList;
import java.util.List;

import Environment.*;

/**
 * During a code review your boss hints that this code could use improvement.
 *
 * Make changes to ensure the code behaves correctly in face of exceptions if
 * appropriate AND simplify the exception handling where possible.
 */

public class Toppings {

	Logger logger = new Logger();

	List<Food> getToppings() {
		String[] ingredients = new String[]{"Tomato Sauce", "Pepperoni", "Cheese"};
		List<Food> toppings = new ArrayList<>();

		try {
			OpenedRefrigerator openFridge = Kitchen.openFridge();
			for (String ingredient : ingredients) {
				Food topping = openFridge.get(ingredient);
				if (topping != null)
					toppings.add(topping);
			}
			openFridge.close();
		} catch (Exception e) {
			logger.log("Could not add topping, continuing with next ingredient", e);
		}

		return toppings;
	}
}
