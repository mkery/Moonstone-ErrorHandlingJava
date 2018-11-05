package Environment;

import java.util.Random;

public class Driver {

	public int getTimeToDelivery() {
		Random rnd = new Random();
		return rnd.nextInt(45) + 15;
	}

	public void deliver(Pizza pizza) {
		/* some code */
	}

}
