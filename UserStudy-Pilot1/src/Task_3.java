import Environment.Airplane;
import Environment.Airplane.DisplayException;
import Environment.Airplane.ExplosionException;
import Environment.Airplane.Location;
import Environment.AirportTower;

public class Task_3 {

    /*
     * Prompt: You are picking up work for a developer on sick leave. They did
     * not get around to creating meaningful exception handlers for the
     * following code. Write exception handlers that are reasonably good quality
     * and behave consistently with other exception handlers in the game code.
     */

    public int landOrCrash(Airplane plane, int windSpeed, int targetX, int targetY) {
        try {
            int x = plane.locX();
            int y = plane.locY();
            int time = Airplane.landPlane(plane, x, y, windSpeed);
            if (time == -1)
                Airplane.simulateCrash(plane.getLocation(), windSpeed);
            else {
                int score = scoreLanding(time, plane.getLocation(), targetX, targetY);
                return score;
            }
        } catch (ExplosionException e) {
            // TODO Notify tower
            AirportTower.notify("Plane crashed short of target due to self destruct");
        } catch (DisplayException e) {
            // TODO Notify refresh screen
            plane.redraw();
        }

        return -1; // plane crashed
    }

    private int scoreLanding(int time, Location loc, int targetX, int targetY) {
        return time * loc.offset(targetX, targetY);
    }
}
