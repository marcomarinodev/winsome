package com.company.client.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {
    // Method invoked from server to notify an event
    public void notifyNewFollower(String username) throws RemoteException;
}
