package Environment;

import java.util.ArrayList;
import java.util.List;

public class Pizza {

	private final Dough dough;
	private final List<Food> toppings = new ArrayList<>();
	boolean isRaw = true;

	public Pizza(Dough dough) {
		this.dough = dough;
	}

	public void addTopping(Food topping) {
		toppings.add(topping);
	}

	public void eat() throws ConsumptionException, TooImpatientException {
		if (isRaw)
			throw new ConsumptionException();
	}

	public boolean isGone() {
		return false;
	}

}
