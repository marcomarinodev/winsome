package com.company.server;

import com.company.server.Exceptions.NonExistingConfigParam;
import com.company.server.Interfaces.ServerAsyncInterface;
import com.company.server.Interfaces.SignInService;
import com.company.server.Utils.PersistentOperator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class CustomServer {

    private String serverAddress;
    private String multicastAddress;
    private int udpPort;
    private int tcpPort;
    private String RMIHostname;
    private int RMIPort;
    private int rewardRate;
    private int autoSaveRate;
    private String asyncServerName;
    private StorageService storageService;
    private final ExecutorService pool;
    private final ServerAsyncImpl asyncServer;
    private ServerAsyncInterface stub;

    public CustomServer(ExecutorService pool) throws RemoteException, AlreadyBoundException {
        this.pool = pool;
        this.asyncServer = new ServerAsyncImpl();
    }

    public void configTest() {
        System.out.println("Server address: " + serverAddress);
        System.out.println("Multicast address: " + multicastAddress);
        System.out.println("UDP Port: " + udpPort);
        // ...
        System.out.println("AUTOMATIC: " + autoSaveRate);
    }

    public void config(String configPathname) throws IOException, NonExistingConfigParam {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configPathname))) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                line = line.strip();
                if (!line.startsWith("#") && !line.equals("")) {

                    if (line.startsWith("SERVER"))
                        this.serverAddress = line.split(" ")[1].strip();
                    else if (line.startsWith("MULTICAST"))
                        this.multicastAddress = line.split(" ")[1].strip();
                    else if (line.startsWith("UDP_PORT"))
                        this.udpPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("TCP_PORT"))
                        this.tcpPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("REG_HOST"))
                        RMIHostname = line.split(" ")[1].strip();
                    else if (line.startsWith("REG_PORT"))
                        this.RMIPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("REW_INTERVAL"))
                        this.rewardRate = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("AUTOMATIC_SAVE"))
                        this.autoSaveRate = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("ASYNC_SERVER"))
                        this.asyncServerName = line.split(" ")[1].strip();
                    else
                        throw new NonExistingConfigParam("Unexpected server config parameter");
                }
            }
            System.out.println("Successful server configuration");
        } catch (FileNotFoundException fe) {
            System.out.println("FILE NOT FOUND");
        }
    }

    public void setStorageService() {
        StorageService storageService = new StorageService("");
        registrationRPC(storageService);
        this.storageService = storageService;
    }

    private void registrationRPC(StorageService storageService) {
        try {
            // Export the object
            SignInService stub = (SignInService) UnicastRemoteObject.exportObject(storageService, 0);

            // Create a registry on specified port
            System.out.println("RMI PORT: " + getRMIPort());
            LocateRegistry.createRegistry(getRMIPort());
            Registry registry = LocateRegistry.getRegistry(getRMIPort());

            // Publish stub in registry
            registry.rebind("RMISignIn", stub);
            System.out.printf("Server ready for %s on %d\n", getRMIHostname(), getRMIPort());
        } catch (RemoteException e) {
            System.out.println("RMI Error: " + e.getMessage());
        }
    }

    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
                            PersistentOperator.persistentWrite(
                                    this.storageService.getStorage(),
                                    this.storageService.getPosts(),
                                    "users.json",
                                    "posts.json");
                            System.out.println("Server is closing...");
                        }
                )
        );
    }

    public void initAsyncServer() throws RemoteException, AlreadyBoundException {
        stub = (ServerAsyncInterface) UnicastRemoteObject.exportObject(asyncServer, getUdpPort());
        String name = getAsyncServerName();
        LocateRegistry.createRegistry(getUdpPort());
        Registry registry = LocateRegistry.getRegistry(getUdpPort());
        registry.bind(name, stub);
        System.out.println("Async server is on");
    }

    public void runServer() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open();
             Selector selector = Selector.open()) {

            // Binding socket to port
            serverSocket.bind(new InetSocketAddress(tcpPort));
            System.out.println("SERVER IS READY ON: " + tcpPort);
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
                            pool.execute(new ReaderThread(key, storageService, selector, asyncServer));
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

    public String getServerAddress() {
        return serverAddress;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public String getRMIHostname() {
        return RMIHostname;
    }

    public int getRMIPort() {
        return RMIPort;
    }

    public int getRewardRate() {
        return rewardRate;
    }

    public int getAutoSaveRate() {
        return autoSaveRate;
    }

    public String getAsyncServerName() {
        return asyncServerName;
    }

    public StorageService getStorageService() {
        return storageService;
    }
}
