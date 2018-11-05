package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class Knight {
	boolean aboutToDie = false;
	boolean died = true;

	public void attack(Monster monster) throws CounterAttackException, KnightDiedException {
		if (died) throw new KnightDiedException();
		monster.receiveDamage(this, 10);
	}

	public void parry() {
		aboutToDie = false;
	}

	public void registerDamage() {
		if (aboutToDie) died = true;
		aboutToDie = true;
	}
}
