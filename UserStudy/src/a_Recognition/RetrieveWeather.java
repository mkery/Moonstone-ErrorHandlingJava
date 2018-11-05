package a_Recognition;

import java.util.ArrayList;
import java.util.List;

import Environment.*;

/**
 * Review the code and add “// HERE” comments to lines that contain code that
 * does not behave correctly in face of exceptions or contains bad exception
 * handling practices.
 */

public class RetrieveWeather {

	Logger logger = new Logger();

	public Weather initWeather(String addr, long date) {

		// Place comments like so:
		Example.badExceptionHandling(); // HERE

		Weather weather = new Weather();
		try {
			Server server = new Server();
			server.openConnection(addr);

			List<String> reportStrings = Weather.getWeather(server, date);
			List<Weather> hourly = new ArrayList<Weather>();

			for (String reportString : reportStrings) {

				try {
					List<Weather> wHours = Weather.parse(reportString);
					for (Weather wHour : wHours) {

						if (wHour == null)
							throw new Exception();
						else
							hourly.add(wHour);

					}
				} catch (Exception e) {
					logger.log("Could not retrieve weather.", e);
					hourly.add(Weather.average(reportStrings));
				}

			}

			server.close();
			weather.setSkySession(hourly);
		} catch (Exception e) {
			weather.setSkyToDefault();
			logger.log(e);
		}

		return weather;
	}

}
