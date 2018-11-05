package d_Writing;

import Environment.*;
import Environment.Airplane.*;

/**
 * You are picking up work for a developer on sick leave. They did not get
 * around to creating meaningful exception handlers for the following code.
 *
 * Write appropriate exception handlers that are in line with other exception
 * handlers in the game code.
 */

public class LandPlane {

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
		} catch (DisplayException e) {
			// TODO Ensure plane is drawn correctly
		}

		return -1; // plane crashed
	}

	private int scoreLanding(int time, Location loc, int targetX, int targetY) {
		return time * loc.offset(targetX, targetY);
	}
}
