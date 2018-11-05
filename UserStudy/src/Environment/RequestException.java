package Environment;

public class RequestException extends Exception {

	private int code;

	public RequestException(int code) {
		this.code = code;
	}

	public RequestException(String message, int code) {
		super(message);
		this.code = code;
	}

	public RequestException(String message, Throwable cause, int code) {
		super(message, cause);
		this.code = code;
	}

	public RequestException(Throwable cause, int code) {
		super(cause);
		this.code = code;
	}

	public RequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int code) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

}
