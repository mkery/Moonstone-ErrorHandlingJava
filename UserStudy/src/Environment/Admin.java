package Environment;

/**
 * Created by florian on 11/15/16.
 */
public class Admin {

	private static Admin sharedAdmin = new Admin();

	public static Admin getSharedAdmin() {
		return sharedAdmin;
	}

	public void notifyAboutServerFailure(Server server, String localizedMessage) {
		/* some code */
	}
}
