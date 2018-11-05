package Environment;

public class RichClient {
	void getFromServer(Server server) throws ConnectionException {
		try {

			server.connect().retrieveData(null);

		} catch (NotFoundException e) {

			// Inform user
			Dialog.showErrorMessage(e);

		}
	}
}
