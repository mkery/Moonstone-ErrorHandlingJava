import Environment.*;

public class Task_C {

    /*
     * You are picking up work for a developer on sick leave. They did not get
     * around to creating meaningful exception handlers for the following code.
     * Write exception handlers that are reasonably good quality and behave
     * consistently with other exception handlers handling the same exceptions.
     */
    void retrieveDataFromServer(Server server, Parameter parameter) {
        try {
            Connection connection = server.connect();
            connection.retrieveData(parameter);
        } catch (ConnectionException e) {
            //TODO Notify admin that Server failed
            Admin.getSharedAdmin().notifyAboutServerFailure(server, "server failed");
        } catch (NotFoundException e) {
            //TODO Inform user
            Dialog.showErrorMessage( e);
        }
    }

}
