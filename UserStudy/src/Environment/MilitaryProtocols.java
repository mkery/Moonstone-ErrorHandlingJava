package Environment;

public class MilitaryProtocols {

	public void selfDestruct(Airplane plane) {
		try {

			if (plane.isArmed()) throw new ExplosionException();

		} catch (ExplosionException e) {
			AirportTower.notify("Plane crashed short of target due to self destruct");
		}
	}

}
