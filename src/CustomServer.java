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

    // server tcp address
    private String serverAddress;
    // udp multicast address
    private String multicastAddress = "239.255.32.32";
    // rmi notification port
    private int notificationPort;
    // udp multicast port
    private int multicastPort = 33333;
    // server tcp port
    private int tcpPort;
    // rmi hostname
    private String RMIHostname;
    // rmi port
    private int RMIPort;
    // reward and auto-save interval
    private int rewardRate;
    // author's earning percentage
    private double authorPercentage;
    // async server name (notifications)
    private String asyncServerName;
    // loggedUsers, storage, posts object
    private StorageService storageService;
    // server's thread pool
    private final ExecutorService pool;
    // async server
    private final ServerAsyncImpl asyncServer;
    private ServerAsyncInterface stub;
    // selector
    Selector selector;

    public CustomServer(ExecutorService pool) throws RemoteException, AlreadyBoundException {
        this.pool = pool;
        this.asyncServer = new ServerAsyncImpl();
    }

    /**
     * configure server
     * @param configPathname configuration file pathname
     * @throws IOException
     * @throws NonExistingConfigParam
     */
    public void config(String configPathname) throws IOException, NonExistingConfigParam {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configPathname))) {
            String line;

            // scanning through the file lines
            while ((line = bufferedReader.readLine()) != null) {
                line = line.strip();

                if (!line.startsWith("#") && !line.equals("")) {

                    if (line.startsWith("SERVER"))
                        this.serverAddress = line.split(" ")[1].strip();
                    else if (line.startsWith("NOTIFICATION_PORT"))
                        this.notificationPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("TCP_PORT"))
                        this.tcpPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("REG_HOST"))
                        RMIHostname = line.split(" ")[1].strip();
                    else if (line.startsWith("REG_PORT"))
                        this.RMIPort = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("REW_INTERVAL"))
                        this.rewardRate = Integer.parseInt(line.split(" ")[1].strip());
                    else if (line.startsWith("ASYNC_SERVER"))
                        this.asyncServerName = line.split(" ")[1].strip();
                    else if (line.startsWith("AUTHOR_PERC"))
                        this.authorPercentage = Double.parseDouble(line.split(" ")[1].strip());
                    else
                        throw new NonExistingConfigParam("Unexpected server config parameter");
                }
            }
            System.out.println("Successful server configuration");
        } catch (FileNotFoundException fe) {
            System.out.println("FILE NOT FOUND");
        }
    }

    /**
     * setup storage service and registration to remote procedure calls
     */
    public void setStorageService() {
        StorageService storageService = new StorageService();
        registrationRPC(storageService);
        this.storageService = storageService;
    }

    /**
     * it handles the registration function with RMI
     * @param storageService
     */
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

    /**
     * perform shutdown
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
                            PersistentOperator.persistentWrite(
                                    this.storageService.storage,
                                    this.storageService.posts,
                                    "users.json",
                                    "posts.json");

                            pool.shutdown();

                            System.out.println("Server is closing...");
                        }
                )
        );
    }

    public void initAsyncServer() throws RemoteException, AlreadyBoundException {
        stub = (ServerAsyncInterface) UnicastRemoteObject.exportObject(asyncServer, getNotificationPort());
        String name = getAsyncServerName();
        LocateRegistry.createRegistry(getNotificationPort());
        Registry registry = LocateRegistry.getRegistry(getNotificationPort());
        registry.bind(name, stub);
        System.out.println("Async server is on");
    }

    public void runServer() {
        try (ServerSocketChannel serverSocket = ServerSocketChannel.open();
             Selector selector = Selector.open()) {

            this.selector = selector;
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

    public String getMulticastAddress() { return multicastAddress; }

    public int getNotificationPort() {
        return notificationPort;
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

    public int getMulticastPort() { return multicastPort; }

    public int getRewardRate() {
        return rewardRate;
    }

    public String getAsyncServerName() {
        return asyncServerName;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public double getAuthorPercentage() {
        return authorPercentage;
    }
}
