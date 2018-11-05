package Environment;

import java.util.Collection;

public class Horde {

	private Collection<Monster> monsters;

	public Collection<Monster> getMonsters() {
		return monsters;
	}

	public void processFightRound(Knight knight) throws KnightDiedException {
		for (Monster monster : monsters) {
			if (monster.isRecovering()) {
				monster.setRecovering(false);
			} else {
				knight.registerDamage();
			}
		}
	}

}
