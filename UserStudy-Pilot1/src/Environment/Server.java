package Environment;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by florian on 11/15/16.
 */
public class Server implements Closeable
{
    public Connection connect() throws ConnectionException {
        return new Connection();
    }

	public void close() throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	public void openConnection(String addr) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	public void reset() throws IOException
	{
		// TODO Auto-generated method stub
		
	}

    public static Connection prepareConnection() {
        // TODO Auto-generated method stub
        return null;
    }
    
	public void displayError(IOException e, String addr) {
		// TODO Auto-generated method stub
		
	}

}
