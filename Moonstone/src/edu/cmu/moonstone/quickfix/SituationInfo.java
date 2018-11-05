package edu.cmu.moonstone.quickfix;

import org.eclipse.jface.text.source.Annotation;

/** Holds the relevant info for a single Bad Catch Situation
 * 
 * @author Michael Puskas
 */
public class SituationInfo 
{
	private String situation;
	private Annotation annotation;

	SituationInfo(String situation, Annotation annotation)
	{
		this.situation = situation;
		this.annotation = annotation;
	}
	
	public String getSituation() 
	{
		return situation;
	}

	public Annotation getAnnotation()
	{
		return annotation;
	}

	public void setAnnotation(Annotation annotation) 
	{
		this.annotation = annotation;
	}
}
