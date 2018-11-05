package d_Writing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Environment.*;

/**
 * You are picking up work for a developer on sick leave. They did not get
 * around to creating meaningful exception handlers for the following code.
 *
 * Write appropriate exception handlers that are in line with other exception
 * handlers in the server's code.
 */

public class RetrieveFromServer {

	List<Map<String, Object>> retrieveDataFromServer(Server server, Parameter parameter) {
		List<Map<String, Object>> data = new ArrayList<>();

		try {
			Connection connection = server.connect();
			connection.setTimeout(30);

			try {
				List<JSON> jsonData = null;
				for (int retry = 0; jsonData == null && retry < 5; retry++) {
					jsonData = connection.retrieveData(parameter);
				}

				for (JSON json : jsonData) {
					data.add(json.toMap());
				}
			} catch (NotFoundException e) {
				// TODO Inform user

			} finally {
				connection.close();
			}

		} catch (ConnectionException e) {
			// TODO Notify admin that Server failed

		}

		return data;
	}

}
