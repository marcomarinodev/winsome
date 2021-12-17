package com.company.server;

import com.company.server.Interfaces.SignInService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerMain {

    public static int port = 12120;
    public static String serviceName = "RMISignIn";

    public static void main(String[] args) {
        System.out.println("Server is running...");

        // TODO: Extract info from the configuration file

        try {
            // Get the user list (from json)
            SignInServiceImpl signInService = new SignInServiceImpl("storage.json");

            // Export the object
            SignInService stub = (SignInService) UnicastRemoteObject.exportObject(signInService, 0);

            // Create a registry on specified port
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);

            // Publish stub in registry
            registry.rebind(serviceName, stub);
            System.out.printf("Server ready for %s on %d\n", serviceName, port);
        } catch (RemoteException e) {
            System.out.println("RMI Error: " + e.getMessage());
        }
    }
}
