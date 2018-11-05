import java.io.IOException;

import Environment.Cat;
import Environment.Logger;
import Environment.Music;
import Environment.Server;

public class Task_1b {
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

    Logger logger = new Logger();

	
	public void catDance(String addr) {
        Server server = new Server();
        try {
            server.openConnection(addr);
            Music music = new Music(Music.getRandomSong(server));
            Cat[] cats = new Cat[5];
            for (Cat c : cats) {
                c.dance(music);
            }
        } catch (IOException e) {
            server.displayError(e, addr);
            logger.log("Server could not be reached for cat dance at " + addr, e);
        } finally {
            try {
                server.close();
            } catch (IOException ignored) {
               //problem
                // ignore
            }
        }

    }
}
