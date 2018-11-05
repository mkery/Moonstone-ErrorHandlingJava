package edu.cmu.moonstone.quickfix;

/** Holds the relevant info for a single Quick Fix resolution.
 * 
 * @author Michael Puskas
 */
public class ResolutionInfo 
{
	private String proposition; //The text that goes on the button, informs user of what resolution will do
	private Resolution resolution; //References the enum contained in Resolution.java
	
	ResolutionInfo()
	{
		proposition = "";
		resolution = null;
	}

	public void setProposition(String Proposition)
	{
		proposition = Proposition;
	}
	
	public void setResolution(Resolution ResolutionNum)
	{
		resolution = ResolutionNum;
	}

	public String getProposition()
	{
		return proposition;
	}
	
	public Resolution getResolution()
	{
		return resolution;
	}
}
