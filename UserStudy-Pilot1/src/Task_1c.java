import java.util.ArrayList;

import Environment.*;

public class Task_1c {

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

    Logger logger;

    public Weather initWeather(String addr, long date) {
        Server server = new Server();
        Weather weather = new Weather();
        try {
            server.openConnection(addr);
            String[] report = Weather.getWeather(server, date);
            ArrayList<Weather> hourly = new ArrayList<Weather>();
            for (String w : report) {
                try {
                    ArrayList<String> wList = Weather.parse(w);
                    for (String t : wList) {
                        if (t == null)
                            throw new Exception();
                        else {
                            Weather wHour = new Weather(t);
                            hourly.add(wHour);
                        }
                    }
                    //problem
                } catch (Exception e) {
                    logger.log("Could not retrieve weather.", e);
                    hourly.add(Weather.average(report));
                }
            }
            server.close();
            weather.setSkySession(hourly);
            //problem
        } catch (Exception e) {
            weather.setSkyToDefault();
            logger.log(e);
        }

        return weather;
    }

}
