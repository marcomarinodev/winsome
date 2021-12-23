package com.company.server;

import com.company.server.Interfaces.SignInService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static final int port = 12120;
    public static final String serviceName = "RMISignIn";
    public static final int timeout = 10000;
    public static final int bufferSize = 32 * 1024;
    static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Server is running...");
        // TODO: Extract info from the configuration file

        SignInServiceImpl signInService = getInService();

        try (ServerSocketChannel serverSocket = ServerSocketChannel.open();
                Selector selector = Selector.open()) {

            // Binding socket to port
            serverSocket.bind(new InetSocketAddress(port + 1));
            // Non-blocking mode
            serverSocket.configureBlocking(false);
            // Register the channel to selector
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started");

            while (true) {
                // Waiting for requests
                selector.select();
                // Get ready keys
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()) {
                    // Get key
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        // There's a new connection
                        if (key.isAcceptable()) {
                            // Accept connection and get client socket channel
                            SocketChannel client = serverSocket.accept();
                            System.out.println("A new client is connected");
                            // Blocking client
                            client.configureBlocking(false);
                            // Register client socket channel in order to read requests
                            client.register(selector, SelectionKey.OP_READ);
                            continue;
                        }

                        if (key.isReadable()) {
                            System.out.println("Client has a request");
                            // I have to assign this key to a thread inside the thread pool
                            key.cancel();
                            pool.execute(new ReaderThread(key, signInService, selector));
                            continue;
                        }

                        if (key.isWritable()) {
                            System.out.println("Server wants to respond");
                            key.cancel();
                            pool.execute(new WriterThread(key,
                                    (String) key.attachment(),
                                    (SocketChannel) key.channel(),
                                    selector));
                        }
                    } catch (IOException e) {
                        System.err.println("Error while serving requests: " + e.getMessage());
                        key.cancel();
                        key.channel().close();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("IOException while opening: " + e.getMessage());
            System.exit(1);
        }

    }

    private static SignInServiceImpl getInService() {
        SignInServiceImpl signInService = new SignInServiceImpl("");
        registrationRPC(signInService);
        return signInService;
    }

    protected static void registrationRPC(SignInServiceImpl signInService) {
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
