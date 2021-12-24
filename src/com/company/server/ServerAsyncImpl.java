package com.company.server;

import com.company.client.interfaces.NotifyEventInterface;
import com.company.server.Interfaces.ServerAsyncInterface;

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

    @Override
    public synchronized void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)) {
            clients.add(clientInterface);
            System.out.println("New client registered to notify service");
        }
    }

    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        if (clients.remove(clientInterface))
            System.out.println("Client unregistered");
        else
            System.out.println("Unable to unregister client");
    }

    public void updateNewFollowers(String newFollower) throws RemoteException { doNewFollowerCallback(newFollower); }

    private synchronized void doNewFollowerCallback(String newFollower) throws RemoteException {
        Iterator i = clients.iterator();

        while (i.hasNext()) {
            NotifyEventInterface client = (NotifyEventInterface) i.next();
            client.notifyNewFollower(newFollower);
        }
    }
}
