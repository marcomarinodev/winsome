import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {
    /**
     * Method invoked from server to notify an incoming new follower
     * @param follower new follower
     * @param followerTagsStr follower tags
     * @param user logged user
     * @throws RemoteException
     */
    public void notifyFollow(String follower, String followerTagsStr, String user) throws RemoteException;

    /**
     * Method invoked from server to notify an unfollow by another user
     * @param follower follower that wants to unfollow user
     * @param user logged user
     * @throws RemoteException
     */
    public void notifyUnfollow(String follower, String user) throws RemoteException;

}
