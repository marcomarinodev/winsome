package com.company.client;

import com.company.client.interfaces.NotifyEventInterface;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
    // new client callback
    public NotifyEventImpl() throws RemoteException {
        super();
    }

    @Override
    public void notifyNewFollower(String username) throws RemoteException {
        System.out.println(username + " started following you");
    }
}
