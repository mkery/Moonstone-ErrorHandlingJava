package edu.cmu.moonstone.view;

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;
import java.util.regex.Pattern;

public class CatchExample {
	private final String path;
	private final String typeName;
	private final String exceptionType;
	private final List<String> methodNames;
	private final List<ASTNode> examples;
	private final boolean isLog;

	private static Pattern loggerPattern = Pattern.compile("\\blog(g(er|ing))?\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public CatchExample(String path, String typeName, String exceptionType, List<String> methodNames, List<ASTNode> examples) {
		this.typeName = typeName;
		this.path = path;
		this.exceptionType = exceptionType;
		this.methodNames = methodNames;
		this.examples = examples;
		isLog = isLog(examples);
	}

	private static boolean isLog(List<ASTNode> examples) {
		return loggerPattern.matcher(examples.get(0).toString()).find();
	}

	public String getExamples() {
		return examples.toString();
	}

	public String getClassPath() {
		return path;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public List<String> getMethods() {
		return methodNames;
	}

	public List<ASTNode> getCode() {
		return examples;
	}

	public int getExampleCount() {
		return methodNames.size();
	}

	public boolean isLog() {
		return isLog;
	}

	@Override
	public String toString() {
		return examples.toString();
	}
}
