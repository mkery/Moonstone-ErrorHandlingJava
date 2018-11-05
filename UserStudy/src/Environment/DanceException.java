package Environment;

import java.io.IOException;

public class DanceException extends IOException {

	public DanceException() {
		super();
	}

	public DanceException(String message) {
		super(message);
	}

	public DanceException(String message, Throwable cause) {
		super(message, cause);
	}

	public DanceException(Throwable cause) {
		super(cause);
	}
}
