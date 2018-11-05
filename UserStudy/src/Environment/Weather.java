package Environment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Weather {


	public Weather(String t) {
		/* some code */
	}

	public Weather() {
		/* some code */
	}

	public static List<String> getWeather(Server s, long date) throws NoConnectionException {
		return Arrays.asList("10am sunny/55F,4pm sunny/60F,10pm sunny/50F", "10am cloudy/50F,4pm cloudy/55F,10pm cloudy/45F");
	}

	public static List<Weather> parse(String w) throws ParseException {
		return Arrays.stream(w.split(",")).map(Weather::new).collect(Collectors.toList());
	}

	public static Weather average(List<String> report) throws ParseException {
		return new Weather();
	}

	public void setSkySession(List<Weather> hourly) {
		/* some code */
	}

	public void setSkyToDefault() {
		/* some code */
	}

	public class NoConnectionException extends Exception {
		/* some code */
	}

	public class ParseException extends Exception {
		/* some code */
	}
}
