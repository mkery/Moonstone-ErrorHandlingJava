package Environment;

public class FlightSchool {

	void trainStart(Airplane plane) {
		try {
			Airplane.simulateGearUp(plane);
		} catch (DisplayException e) {
			plane.redraw();
		}
	}

}
