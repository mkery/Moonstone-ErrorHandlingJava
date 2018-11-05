package Environment;

import java.util.ArrayList;
import java.util.List;

public class DoughBowl {

	private List<Food> foods = new ArrayList<>();
	private Dough dough;

	public void add(Food food) throws OverflowException {
		dough = null;
		foods.add(food);
		if (foods.size() > 4)
			throw new OverflowException();
	}

	public void mix() {
		dough = new Dough();
	}

	public Dough getDough() throws NotMixedException {
		if (dough == null)
			throw new NotMixedException();
		return dough;
	}

}
