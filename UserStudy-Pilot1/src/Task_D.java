import Environment.*;
import static Environment.DataRequest.setInvoiceData;

public class Task_D {

    /*
     * Prompt: A junior software developer wrote the following code addition to
     * a central component of a software system. You are tasked with making sure
     * it will not have any adverse effects in production. Make changes to
     * ensure correct exception handling where appropriate.
     */
    void retrieveInvoice() throws ServerFailedException {
        Connection connection = Server.prepareConnection();
        DataRequest request = null;
        try {
            request = new DataRequest(connection, "Invoice");
            setInvoiceData(request.execute());
        } catch (RequestException e) {
            if (e.getCode() != 404) {
                //connection.close();
                throw new ServerFailedException(e);
            }
            setInvoiceData(null);
        } finally {
            request.dispose();
            connection.close();
        }
    }

}
