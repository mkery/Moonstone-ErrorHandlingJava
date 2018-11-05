package Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by florian on 11/15/16.
 */
public class Monster {
	private final String name;
	private int health = 100;
	private boolean recovering = false;

	private static List<String> allAvailableMonsters = Arrays.asList("gremlin", "Davy Jones", "Chupacabra", "manticore", "banshee");

	public Monster(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getHealth() {
		return health;
	}

	public boolean isRecovering() {
		return recovering;
	}

	public void setRecovering(boolean recovering) {
		this.recovering = recovering;
	}

	public void receiveDamage(Knight knight, int damage) throws CounterAttackException {
		boolean wasAlive = health > 0;

		health = Math.max(health - damage, 0);
		recovering = true;

		if (wasAlive) {
			knight.registerDamage();
			throw new CounterAttackException();
		}
	}

	public static List<String> loadMonsters(Server server, int player_level, Place setting) throws UnavailableException {
		return allAvailableMonsters;
	}


	public static Monster randomEncounter(List<String> monsterDex, Place setting, int player_level) {
		int idx = new Random().nextInt(monsterDex.size());
		return new Monster(monsterDex.get(idx));
	}

	public Attack randomAttack() {
		return new Attack();
	}

	public void render() {
		/* some code */
	}

	public void displayAttackMove() throws DisplayException {
		/* some code */
	}
}
