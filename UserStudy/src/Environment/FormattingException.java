package Environment;

/**
 * Created by florian on 2/2/17.
 */
public class FormattingException extends ContentException {
	public FormattingException() {
		super();
	}

	public FormattingException(String message) {
		super(message);
	}

	public FormattingException(String message, Throwable cause) {
		super(message, cause);
	}

	public FormattingException(Throwable cause) {
		super(cause);
	}

	public FormattingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
