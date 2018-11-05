package edu.cmu.moonstone.quickfix;

/** Holds the Title and body for a marker's description
 * 
 * @author Michael Puskas
 */
public class Description 
{
	public final String body; //Body of description to be used in the text box
	public final String title; //Top of description to be used in the text box
	public final String header; //Header to be used in the top left label
	public final String url; //URL of the webpage to link to from the "more" button
	
	public Description(String body, String title, String header, String url)
	{
		this.body = body;
		this.title = title;
		this.header = header;
		this.url = url;
	}
}
