import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface SignInService extends Remote {
    /**
     * @param username not empty username text
     * @param password password with constraints: at least 8 chars
     * @param tags content tags (max 5)
     * @return an integer (200: success, 455: failure)
     */
    public String register(String username, String password, ArrayList<String> tags) throws RemoteException;
}
