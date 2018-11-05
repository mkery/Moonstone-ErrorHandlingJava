import java.util.ArrayList;
import java.util.List;

import Environment.*;

public class Task_A {

    /*
     * Your team has to implement code that makes some pizza. Your colleague
     * found this code on the Internet and suggests to use it as a starting
     * point. You're skeptical whether its reliable, when something goes wrong.
     * 
     * Make appropriate changes to the exception handling code to ensure the
     * best possible outcome and log all problems, so an operations manager can
     * make adjustments.
     */

    void makePizza(Driver driver) {
        Pantry pantry = Kitchen.getSharedPantry();
        Tap tap = Kitchen.getSharedTap();

        Food flour = pantry.get("Flour");
        Water water = tap.getWater();
        Food olives = pantry.get("Olives");

        DoughBowl bowl = new DoughBowl();
        try {
            bowl.add(flour);
            bowl.add(water);
            bowl.add(olives);
            bowl.mix();
        } catch (OverflowException e) {
            System.out.println("overflow exception" +e);
            Kitchen.cleanupCounter();
        }

        Dough dough = null;
        try {
            dough = bowl.getDough();
        } catch (NotMixedException e) {
            throw new AssertionError("Programmer error: Dough should always be mixed", e);
        }

        Refrigerator fridge = Kitchen.getSharedFridge();
        String[] ingredients = new String[] { "Tomato Sauce", "Pepperoni", "Cheese" };
        List<Food> toppings = new ArrayList<>();
        try {
            OpenedRefrigerator openFridge = fridge.open();
            for (String ingredient : ingredients) {
                toppings.add(openFridge.get(ingredient));
            }
            openFridge.close();
        } catch (UnavailableException e) {
            System.out.println(
                    "We have to skip " + e.getUnavailableFood().toString() + ", continuing with next ingredient");
        }

        Pizza pizza = new Pizza(dough);
        for (Food topping : toppings)
            pizza.addTopping(topping);

        Oven oven = Kitchen.getSharedOven();
        try {
            oven.bake(pizza, driver.getTimeToDelivery());
            pizza.eat();
        } catch(PizzaBurntException e) {
            System.out.println("PizzaBurntException" +e.getLocalizedMessage());
        } catch (ConsumptionException e) {
            System.out.println("ConsumptionException" + e.getLocalizedMessage());
        } catch (TooImpatientException e) {
            System.out.println("TooImpatientException" + e.getLocalizedMessage());
        }

    }

}
