package com.company.server.Interfaces;

import com.company.client.interfaces.NotifyEventInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerAsyncInterface extends Remote {
    // callback registration
    public void registerForCallback (NotifyEventInterface clientInterface) throws RemoteException;

    // cancel callback registration
    public void unregisterForCallback (NotifyEventInterface clientInterface) throws RemoteException;
}
