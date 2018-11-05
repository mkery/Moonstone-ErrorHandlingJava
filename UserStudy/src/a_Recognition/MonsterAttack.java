package a_Recognition;

import java.util.List;

import Environment.*;

/**
 * Review the code and add “// HERE” comments to lines that contain code that
 * does not behave correctly in face of exceptions or contains bad exception
 * handling practices.
 */

public class MonsterAttack {

	public int monsterAttack(Server server, int player_level, Place setting) throws Exception {

		// Place comments like so:
		Example.badExceptionHandling(); // HERE

		List<String> monsterDex = null;
		try {
			monsterDex = Monster.loadMonsters(server, player_level, setting);
			if (monsterDex.isEmpty())
				return 0;
		} catch (UnavailableException e) {

		}

		Monster mon = Monster.randomEncounter(monsterDex, setting, player_level);
		Attack att = mon.randomAttack();
		int damage = att.getDamage();

		try {
			mon.render();
			mon.displayAttackMove();
		} catch (Throwable e) {
			// reset connection and report failure
			server.reset();
			throw new DisplayFailedException();
		}
		return damage;
	}
}
