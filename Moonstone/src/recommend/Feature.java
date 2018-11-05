package recommend;

public class Feature {
	
	private String featureName;
	private String featureClass;
	
	public static final String EXCEPTION_TYPE = "ExceptionType";
	public static final String CODE_ORIGIN_FILE = "File";
	public static final String HANDLER_TYPE = "HandlerType";
	public static final String CATCH = "0";
	public static final String FINALLY = "1";
	public static final String LOCATION = "loc";
	public static final String RESOURCE = "resource";

	public Feature(String name, String className)
	{
		featureName = name;
		featureClass = className;
	}

	public String getFeatureName() {
		return featureName;
	}

	public String getFeatureClass() {
		return featureClass;
	}
}
