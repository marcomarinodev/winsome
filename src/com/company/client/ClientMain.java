package com.company.client;

import com.company.client.interfaces.NotifyEventInterface;
import com.company.server.Interfaces.ServerAsyncInterface;
import com.company.server.SignInHandler;
import com.company.server.Utils.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    public static int port = 12120;
    public static int udpPort = 12122;
    public static String serviceName = "RMISignIn";
    public static final String hostname = "localhost";
    public static final int bufferSize = 32 * 1024;
    public static String loggedUsername = "";
    public static List<Pair<String, String>> followers = new ArrayList<>();
    public static ServerAsyncInterface server = null;
    public static NotifyEventInterface stub = null;
    public static WalletReceiver walletReceiver = new WalletReceiver(udpPort, 1024, "239.255.32.32");
    public static NotifyEventInterface callbackObj;

    public static void main(String[] args) {
        SocketChannel cl;
        Boolean exit = false;

        try {
            try (SocketChannel client = SocketChannel.open(new InetSocketAddress(hostname, port + 1))) {
                cl = client;
                while (!exit) {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("> ");
                    String[] newArgs = scanner.nextLine().split(" ");
                    exit = handleOperations(newArgs, cl);
                }
                System.out.println("< Closing client connection");
                cl.close();
            } catch (Exception e) {
                System.out.println("< Server is not reachable");
            } finally {
                stopAsyncThreads();
            }
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            stopAsyncThreads();
        }

        System.out.println("Closing client");
    }

    public static Boolean handleOperations(String[] args, SocketChannel client) {

        if (args.length < 1) return false;
        String operation = args[0];

        if (operation.equals("register"))
            return handleRegistration(args);

        if (operation.equals("list"))
            return handleListOperation(client, args);

        if (operation.equals("login"))
            return handleLogin(client, args);

        if (operation.equals("logout"))
            return handleLogout(client, args);

        if (operation.equals("follow") || operation.equals("unfollow"))
            return handleFollow(client, args);

        if (operation.equals("post"))
            return handlePost(client, args);

        if (operation.equals("show"))
            return handleShowOperation(client, args);

        if (operation.equals("rate"))
            return handleRate(client, args);

        if (operation.equals("blog"))
            return handleBlog(client, args);

        if (operation.equals("delete"))
            return handleDelete(client, args);

        if (operation.equals("rewin"))
            return handleRewin(client, args);

        if (operation.equals("comment"))
            return handleComment(client, args);

        if (operation.equals("wallet"))
            return handleWallet(client, args);

        if (operation.equals("exit"))
            return handleExit(client, args);

        System.out.println("< " + operation + " is not a permitted operation");

        return false;
    }

    private static Boolean handleExit(SocketChannel client, String[] args) {
        sendRequest(client, args);
        stopAsyncThreads();
        return true;
    }

    private static Boolean handleWallet(SocketChannel client, String[] args) {
        sendRequest(client, args);
        return false;
    }

    private static Boolean handleComment(SocketChannel client, String[] args) {
        // TODO: Add client side checks
        sendRequest(client, args);
        return false;
    }

    private static Boolean handleRewin(SocketChannel client, String[] args) {
        if (args.length < 2) System.out.println("< you must specify the post id");
        else sendRequest(client, args);
        return false;
    }

    private static Boolean handleDelete(SocketChannel client, String[] args) {
        if (args.length < 2) System.out.println("< you must specify the post id");
        else sendRequest(client, args);
        return false;
    }

    private static Boolean handleBlog(SocketChannel client, String[] args) {
        sendRequest(client, args);
        return false;
    }

    private static Boolean handleRate(SocketChannel client, String[] args) {
        if (args.length < 3) System.out.println("< you must specify post id and your vote");
        else sendRequest(client, args);
        return false;
    }

    private static Boolean handleShowOperation(SocketChannel client, String[] args) {
        if (args.length < 2) System.out.println("< you're missing show arguments");
        if (!(args[1].equals("post") || args[1].equals("feed")))
            System.out.println("< " + args[1] + " is not a show option");
        else sendRequest(client, args);
        return false;
    }

    private static Boolean handlePost(SocketChannel client, String[] args) {
        // TODO: Add client side checks
        sendRequest(client, args);
        return false;
    }

    private static Boolean handleFollow(SocketChannel client, String[] args) {
        if (args.length < 2) {
            System.out.println("< you must specify a user");
        } else sendRequest(client, args);
        return false;
    }

    private static Boolean handleListOperation(SocketChannel client, String[] args) {
        if (args.length < 2) {
            System.out.println("< list without parameters is not supported");
        } else {
            if (args[1].equals("followers")) return handleListFollowers();
            if (!(args[1].equals("users") || args[1].equals("following"))) {
                System.out.println("< list " + args[1] + " is not supported");
            } else sendRequest(client, args);

        }
        return false;
    }

    private static Boolean handleLogout(SocketChannel client, String[] args) {
        loggedUsername = "";
        followers.clear();
        stopAsyncThreads();
        sendRequest(client, args);
        return false;
    }

    private static boolean handleLogin(SocketChannel client, String[] args) {
        if (args.length > 1) {
            loggedUsername = args[1];
        }

        String response = sendRequest(client, args);

        // Cycle until penultimate row
        String[] splitResponse = response.split("//");

        for (String str: splitResponse) {
            String[] splitLine = str.split("/");
            if (splitLine.length > 1)
                followers.add(new Pair<>(splitLine[0], splitLine[1]));
        }

        if (splitResponse[splitResponse.length - 1].equals("< " + loggedUsername + " logged in")) {
            // Register for notifications
            try {
                // Searching notification server
                Registry registry = LocateRegistry.getRegistry(5000);
                String name = "AsyncServer";
                server = (ServerAsyncInterface) registry.lookup(name);
                // registering for callback
                System.out.println("< REGISTERED FOR NOTIFICATIONS");
                callbackObj = new NotifyEventImpl(followers, loggedUsername);
                stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
                server.registerForCallback(stub);
            } catch (Exception e) {
                System.err.println("Client exception " + e.getMessage());
            }

            walletReceiver.start();
        }

        return false;
    }

    private static String sendRequest(SocketChannel client, String[] args) {
        String request = String.join(" ", args);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        try {
            // Write request length
            byte[] message = request.getBytes();
            buffer.clear();
            buffer.putInt(message.length);
            buffer.put(message);
            buffer.flip();
            client.write(buffer);

            // Read response
            buffer.clear();
            client.read(buffer);
            buffer.flip();
            int receivedLength = buffer.getInt();
            byte[] receivedBytes = new byte[receivedLength];
            buffer.get(receivedBytes);
            String response = new String(receivedBytes);
            System.out.println(response);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return "< IOException occurred!";
        }
    }

    private static boolean handleListFollowers() {
        if (loggedUsername.equals("")) {
            System.out.println("< You must login before do this action");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("< \tUser\t|\tTag\n");
            stringBuilder.append("< —------------------------------------\n");
            for (Pair<String, String> follower : followers)
                stringBuilder.append("< ").append(follower.getLeft()).append("\t|\t").append(follower.getRight()).append("\n");
            System.out.println(stringBuilder);
        }
        return false;
    }

    private static Boolean handleRegistration(String[] args) {
        try {
            SignInHandler signInHandler = new SignInHandler(args);
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static void stopAsyncThreads() {
        try {
            if (server != null && stub != null) {
                System.out.println("Unregistering");
                server.unregisterForCallback(stub);
                UnicastRemoteObject.unexportObject(callbackObj, true);
            }
            if (walletReceiver.worker != null)
                walletReceiver.stop();
        } catch (RemoteException re) {
            System.out.println(re);
        }
    }
}
