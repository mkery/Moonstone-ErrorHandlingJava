package b_Fix;

import java.util.List;

import Environment.*;

/**
 * Your team maintains this legacy code for making pizza. A new company policy
 * calls for review of old exception handling code.
 *
 * Please check this code and make changes where needed to ensure that it avoids
 * bad exception handling practices AND simplify the exception handling where
 * possible.
 */

public class PizzaPlace {

	Logger logger = new Logger();

	void havePizza(Driver driver, Dough dough, List<Food> toppings) {
		Pizza pizza = new Pizza(dough);
		for (Food topping : toppings)
			pizza.addTopping(topping);

		Oven oven = Kitchen.getSharedOven();
		try {
			oven.bake(pizza, driver.getTimeToDelivery());
			driver.deliver(pizza);

			Diary diary = Diary.getDiary();
			diary.savePicture("Yummy pizza");

			while (!pizza.isGone())
				pizza.eat();

			diary.savePicture("Empty box");
		} catch (Exception e) {
			logger.log("Following Exception occurred while having pizza: ", e);
		}
	}

}
