import java.util.ArrayList;
import Environment.Attack;
import Environment.DisplayFailedException;
import Environment.Monster;
import Environment.Place;
import Environment.Server;

public class Task_1a {

    /*
     * Prompt: Your boss is unsatisfied with the overall quality of error
     * handling in the project’s code. You have been instructed to do code
     * review across the project to specifically review the error handling. You
     * will see a series of 5 code snippets, and have a minute to identify any
     * problems in each and add a comment “// problem” on the line locating the
     * problem.
     * 
     * A snippet may have several problems, or none at all.
     */

    public int monsterAttack(Server server, int player_level, Place setting) throws Exception {
        ArrayList<String> monsterDex = null;
        try {
            monsterDex = Monster.loadMonsters(server, player_level, setting);
        // use more specific exception handler
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (monsterDex.isEmpty())
            return 0;

        Monster mon = Monster.randomEncounter(monsterDex, setting, player_level);
        Attack att = mon.randomAttack();
        int damage = att.getDamage();

        try {
            mon.render();
            mon.displayAttackMove();
        // problem
        } catch (Throwable e) {
            // reset connection and retry once
            server.reset();
            try {
                mon.render();
                mon.displayAttackMove();
                // problem

            } catch (Throwable f) {
                // problem
                throw new DisplayFailedException();
            }
        }
        return damage;
    }
}
