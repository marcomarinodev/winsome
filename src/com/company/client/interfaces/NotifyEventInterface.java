package com.company.client.interfaces;

import com.company.server.Utils.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {
    /**
     * @param follower new follower
     * @throws RemoteException
     */
    // Method invoked from server to notify an event
    public void notifyNewFollower(String follower, String followerTagsStr, String user) throws RemoteException;
}
