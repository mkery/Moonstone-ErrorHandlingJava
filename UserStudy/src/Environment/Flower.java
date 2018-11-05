package Environment;

import java.awt.*;

public class Flower {

	public Color getColor() {
		return Color.RED;
	}

	public Sample getSample() {
		return new Sample();
	}

	public void buy() throws OutOfStockException {
		/* some code */
	}

}
