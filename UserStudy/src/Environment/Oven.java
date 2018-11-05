package Environment;

public class Oven {

	public void bake(Pizza pizza, int minutes) throws PizzaBurntException {
		if (minutes > 30) throw new PizzaBurntException();
		if (minutes > 20) pizza.isRaw = false;
	}

}
