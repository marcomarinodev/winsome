package com.company.client;

import com.company.client.interfaces.NotifyEventInterface;
import com.company.server.Utils.Pair;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
    private String username;
    private List<Pair<String, String>> followers;
    // new client callback
    public NotifyEventImpl(List<Pair<String, String>> followers, String username) throws RemoteException {
        super();
        this.username = username;
        this.followers = followers;
    }

    @Override
    public void notifyFollow(String follower, String followerTagsStr, String username) throws RemoteException {
        if (this.username.equals(username)) {
            followers.add(new Pair<>(follower, followerTagsStr));
            System.out.println(follower + " started following you\n");
        }
    }

    @Override
    public void notifyUnfollow(String follower, String username) throws RemoteException {
        if (this.username.equals(username)) {
            followers.removeIf(foll -> foll.getLeft().equals(follower));
            System.out.println(follower + " stopped following you\n");
        }
    }

    public String getUsername() {
        return username;
    }
}
