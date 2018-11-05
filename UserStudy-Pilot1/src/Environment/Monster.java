package Environment;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import Environment.Airplane.DisplayException;

/**
 * Created by florian on 11/15/16.
 */
public class Monster {
    private int health = 100;
    private boolean recovering = false;

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

	public static ArrayList<String> loadMonsters(Server server, int player_level, Place setting) throws UnavailableException
	{
		// TODO Auto-generated method stub
		return null;
	}


	public static Monster randomEncounter(ArrayList<String> monsterDex, Place setting, int player_level) {
		// TODO Auto-generated method stub
		return null;
	}

	public Attack randomAttack() {
		// TODO Auto-generated method stub
		return null;
	}

	public void render() {
		// TODO Auto-generated method stub
		
	}

	public void displayAttackMove() throws DisplayException {
		// TODO Auto-generated method stub
		
	}
}
