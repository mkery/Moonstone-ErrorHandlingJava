import Environment.Logger;
import Environment.Player;
import Environment.Player.ItemsException;
import Environment.Player.LoadException;
import Environment.Player.PlayerException;

public class Task_2 {

    /*
     * Prompt: Your development team is frustrated with the low-quality log
     * messages coming from vital parts of the game’s code. Fix the following
     * code so that a descriptive message is logged that states precisely what
     * went wrong when an exception occurs.
     */

    Logger logger = new Logger();

    public boolean loadPlayer(Player player) {
        try {
            player.loadAvatar();
            player.loadVoice();
            player.updateItems();
            player.addWeapons();
            player.addHorse();
            player.walkCycle();
            player.introCycle();
            return true;
        } catch (ItemsException e) {
            logger.log("Item exception", e);
            return false;
        } catch (LoadException  e) {
            logger.log("LoadException" , e);
            return false;
        } catch (PlayerException e) {
            logger.log("An exception!", e);
            return false;
        }
    }
}
