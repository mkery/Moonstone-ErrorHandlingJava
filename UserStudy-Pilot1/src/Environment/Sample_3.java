package Environment;

import Environment.Airplane.DisplayException;
import Environment.Airplane.ExplosionException;

public class Sample_3 {

    public void selfDestruct(Airplane plane) {
        try {

            if (false) throw new ExplosionException();
            throw new DisplayException();

        } catch (ExplosionException e) {
            AirportTower.notify("Plane crashed short of target due to self destruct");
        } catch (DisplayException e) {
            plane.redraw();
        }
    }
    
}
