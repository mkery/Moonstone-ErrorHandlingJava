package Environment;

public class Broker {
	private Connection pooledConnection;

	void poolConnection(Server server) {
		try {

			pooledConnection = server.connect();

		} catch (ConnectionException e) {

			// Notify admin that Server failed
			Admin admin = Admin.getSharedAdmin();
			admin.notifyAboutServerFailure(server, e.getLocalizedMessage());

		}
	}
}
