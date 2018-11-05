package edu.cmu.moonstone.quickfix;

/** A class to hold the enum for quick fix resolutions
 * 
 * @author Michael Puskas
 */
public enum Resolution
{
	SPECIFIC_CATCH, SEPARATE_CATCH, REMOVE_THROW_FROM_TRY, REMOVE_THROW_FROM_CATCH, ADD_STACKTRACE, ADD_LOGGER, ADD_EH_RECOMMENDATION, IGNOREEMPTYCATCH
	, FIX_DESTRUCTIVE_WRAP, TRY_WITH_RESOURCES, IGNORE, REMOVE_CATCH
}
