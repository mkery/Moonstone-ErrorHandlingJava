package b_Fix;

import static Environment.DataRequest.setInvoiceData;

import Environment.*;

/**
 * This code was written by a junior developer, and effects a vital part of the
 * software.
 * 
 * Please check this code and make changes where needed to ensure that it avoids
 * bad exception handling practices and behaves correctly in face of exceptions.
 */

public class Invoice {

	void retrieveInvoice() throws ServerFailedException {
		Connection connection = Server.prepareConnection();
		DataRequest request = null;

		try {
			request = new DataRequest(connection, "Invoice");
			setInvoiceData(request.execute());

			connection.close();
		} catch (RequestException e) {
			throw new ServerFailedException(e);
		} finally {
			if (request != null)
				request.dispose();
		}
	}

}
