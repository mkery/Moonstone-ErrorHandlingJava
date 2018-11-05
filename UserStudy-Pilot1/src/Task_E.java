import java.awt.Color;

import Environment.*;

public class Task_E {

    /*
     * Prompt: Add line “flower.buy()” to line 23. Extend the existing exception
     * handler as necessary and fix any issues that might result from the newly
     * introduced control flow.
     */

    Logger logger;

    void visitFlowerShop() {
        FlowerInventory inventory = null;
        try {
            inventory = FlowerShop.getInventory();
            for (Flower flower : inventory.getFlowers()) {
                if (flower.getColor() == Color.RED) {
                    Sample sample = flower.getSample();
                    if (sample.getSmell() > 5) {
                        // here
                    }
                    sample.returnSample();
                }
            }
            inventory.close();
        } catch (ShopClosedException e) {
            logger.log("Could not buy flowers", e);
        }
    }

}
