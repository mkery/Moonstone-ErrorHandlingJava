package Environment;

import java.io.IOException;

public class Email implements Appendable {

	@Override
	public Email append(CharSequence csq) {
		return null;
	}

	@Override
	public Email append(CharSequence csq, int start, int end) {
		return null;
	}

	@Override
	public Email append(char c) {
		return new Email();
	}

	public void setRecipient(Customer customer) {
		/* some code */
	}

	public void send() throws IOException {
		/* some code */
	}

}
