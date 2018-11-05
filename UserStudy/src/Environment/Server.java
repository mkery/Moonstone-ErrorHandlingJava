package Environment;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by florian on 11/15/16.
 */
public class Server implements Closeable {
	public Connection connect() throws ConnectionException {
		return new Connection();
	}

	public void close() throws IOException {
		/* some code */
	}

	public void openConnection(String addr) throws IOException {
		/* some code */
	}

	public void reset() throws IOException {
		/* some code */
	}

	public static Connection prepareConnection() {
		return new Connection();
	}

	public void displayError(IOException e, String addr) {
		/* some code */
	}

}
