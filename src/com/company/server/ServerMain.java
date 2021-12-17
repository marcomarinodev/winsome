package com.company.server;

import com.company.server.Interfaces.SignInService;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerMain {

    public static int port = 12120;
    public static String serviceName = "RMISignIn";

    public static void main(String[] args) throws Exception {
        System.out.println("Server is running...");

        // TODO: Extract info from the configuration file

        // Get the user list (from json)
        SignInServiceImpl signInService = new SignInServiceImpl("storage.json");
        registrationRPC(signInService);

        try (ServerSocket listener = new ServerSocket(12121)) {
            ExecutorService pool = Executors.newCachedThreadPool();
            while (true) {
                pool.execute(new WorkerThread(listener.accept()));
            }
        }

    }

    private static void registrationRPC(SignInServiceImpl signInService) {
        try {
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
