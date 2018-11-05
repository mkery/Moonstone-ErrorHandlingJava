import java.util.Collection;
import Environment.*;

public class Task_B {

    /*
     * You're reviewing code for a game. This method was written to fend off all
     * attacking monsters. Verify that it works as intended and make appropriate
     * changes to the exception handling code where necessary.
     */
    // TODO: Leave out?
    void fightMonsters(Horde horde) {
        Knight knight = new Knight();

        try {
            Collection<Monster> monsters = horde.getMonsters();

            while (!monsters.isEmpty()) {

                try {
                    for (Monster monster : monsters) {
                        knight.attack(monster);
                    }
                } catch (CounterAttackException e) {
                    knight.parry();
                }

                monsters.removeIf(monster -> monster.getHealth() == 0);
                horde.processFightRound(knight);
            }

        } catch (KnightDiedException e) {
            throw new AssertionError("Programmer error: The knight should not die in this method", e);
        }

        System.out.println("The king prevailed");
    }

}
