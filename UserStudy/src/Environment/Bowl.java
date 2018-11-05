package Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by florian on 11/15/16.
 */
public class Bowl {
	private List<Food> foods = new ArrayList<>();

	public void add(Food food) {
		foods.add(food);
	}
}
