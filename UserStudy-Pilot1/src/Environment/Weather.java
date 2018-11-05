package Environment;

import java.util.ArrayList;

public class Weather 
{
	
	
	
	public Weather(String t) {
		// TODO Auto-generated constructor stub
	}

	public Weather() {
		// TODO Auto-generated constructor stub
	}

	public static String[] getWeather(Server s, long date) throws NoConnectionException
	{
		return null;
	}

	public static ArrayList<String> parse(String w) throws ParseException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public static Weather average(String[] report) throws ParseException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void setSkySession(ArrayList<Weather> hourly) {
		// TODO Auto-generated method stub
		
	}

	public void setSkyToDefault() {
		// TODO Auto-generated method stub
		
	}
	
	public class NoConnectionException extends Exception
	{
		
	}
	
	public class ParseException extends Exception
	{
		
	}
}
