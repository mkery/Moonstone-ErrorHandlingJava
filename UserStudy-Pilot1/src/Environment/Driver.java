package Environment;

import java.util.Random;

public class Driver {

    public int getTimeToDelivery() {
        Random rnd = new Random();
        return rnd.nextInt(45) + 15;
    }

}
