package Environment;

public class Sample_C {
    void interactWithServer(Server server) {
        try {
            
            if (false) throw new ConnectionException();
            throw new NotFoundException();
            
        } catch (ConnectionException e) {
            
            // Notify admin that Server failed
            Admin admin = Admin.getSharedAdmin();
            admin.notifyAboutServerFailure(server, e.getLocalizedMessage());
            
        } catch (NotFoundException e) {
            
            // Inform user
            Dialog.showErrorMessage(e);
            
        }
    }
}
