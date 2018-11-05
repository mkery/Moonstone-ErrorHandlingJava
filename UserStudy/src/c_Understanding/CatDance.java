package c_Understanding;

import java.io.IOException;

import Environment.*;

/**
 * You are building a game for children. The following code's purpose is to
 * trigger the 5 cat avatars in the game to dance. The first 2 cats dance
 * perfectly, however the third cat, Mr. Whiskers, sometimes fails the dance.
 *
 * You're trying to figure out, how the game is affected: If the 3rd cat's dance
 * throws an exception, how many cats bow?
 *
 * Answer =
 */

public class CatDance {

	Logger logger = new Logger();

	public void catDance(String addr) {
		try (Server server = new Server()) {
			server.openConnection(addr);
			Music music = new Music(Music.getRandomSong(server));
			Cat[] cats = Cat.getFiveCats();
			for (Cat c : cats) {
				try {
					c.dance(music);
				} finally {
					c.bow();
				}
			}
		} catch (IOException e) {
			logger.log("Server could not be reached for cat dance at " + addr, e);

		}
	}

}
