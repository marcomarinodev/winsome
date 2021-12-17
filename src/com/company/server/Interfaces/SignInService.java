package com.company.server.Interfaces;

import com.company.SystemCodes;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SignInService extends Remote {
    /**
     * @param username not empty username text
     * @param password password with constraints: at least 8 chars
     * @param tags content tags (max 5)
     * @return an integer (200: success, 455: failure)
     */
    public String register(String username, String password, String[] tags) throws RemoteException;

    /**
     * @param username not empty username text
     * @param password account's password
     * @return an integer (200: success, 4:
     */
    public String login(String username, String password) throws RemoteException;

    /**
     * @param username not empty username text
     */
    public void logout(String username) throws RemoteException;
}
