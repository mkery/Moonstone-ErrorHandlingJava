package b_Fix;

import Environment.*;

/**
 * The following code maintains a statistic about the amount of exception
 * encountered. Your team is concerned about the frequency, but is so far unable
 * to tell which kind of exceptions occur.
 *
 * Fix the code so that if an exception occurs, its most SPECIFIC type is
 * recorded in the statistic.
 */

public class LoadPlayer {

	ExceptionStatistic statistic = new ExceptionStatistic();

	public boolean loadPlayer(Player player) {
		try {
			player.introCycle();
			player.walkCycle();
			player.loadAvatar();
			player.loadVoice();
			player.addWeapons();
			player.addHorse();
			player.updateItems();
			return true;
		} catch (Exception e) {
			statistic.recordUnknownException();
		}

		return false;
	}
}
