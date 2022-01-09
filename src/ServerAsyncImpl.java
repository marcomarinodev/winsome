import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerAsyncImpl extends RemoteObject implements ServerAsyncInterface {
    // list of registered clients
    private List<NotifyEventInterface> clients;

    public ServerAsyncImpl() throws RemoteException {
        super();
        clients = new ArrayList<>();
    }

    // This function registers for callback a new client with the precondition that the client in parameter
    // is not already registered for callback
    @Override
    public synchronized void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)) {
            clients.add(clientInterface);
            System.out.println("New client registered to notify service");
        }
    }

    // This function unregisters for callback a new client with the precondition that the client in parameter
    // is registered for the callback
    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        System.out.println(clients);
        if (clients.remove(clientInterface))
            System.out.println("Client unregistered");
        else
            System.out.println("Unable to unregister client");
    }

    /**
     * RMI callback to update new incoming follower
     * @param newFollower incoming follower
     * @param newFollowerTagsStr incoming follower's tags
     * @param receiver
     * @throws RemoteException
     */
    public void updateNewFollower(String newFollower, String newFollowerTagsStr, String receiver) throws RemoteException {
        doFollowCallback(newFollower, newFollowerTagsStr, receiver);
    }

    /**
     * RMI callback to say that exFollower stopped following you
     * @param exFollower old follower
     * @param receiver
     * @throws RemoteException
     */
    public void updateExFollower(String exFollower, String receiver) throws RemoteException {
        doUnfollowCallback(exFollower, receiver);
    }

    private synchronized void doFollowCallback(String newFollower, String newFollowerTagsStr, String receiver) throws RemoteException {
        Iterator i = clients.iterator();

        while (i.hasNext()) {
            NotifyEventInterface client = (NotifyEventInterface) i.next();
            client.notifyFollow(newFollower, newFollowerTagsStr, receiver);
        }
    }

    private synchronized void doUnfollowCallback(String follower, String receiver) throws RemoteException {
        Iterator i = clients.iterator();

        while (i.hasNext()) {
            NotifyEventInterface client = (NotifyEventInterface) i.next();
            client.notifyUnfollow(follower, receiver);
        }
    }
}
