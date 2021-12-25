package com.company.client;

import com.company.client.interfaces.NotifyEventInterface;
import com.company.server.Interfaces.ServerAsyncInterface;
import com.company.server.SignInHandler;
import com.company.server.Utils.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    public static int port = 12120;
    public static String serviceName = "RMISignIn";
    public static final String hostname = "localhost";
    public static final int bufferSize = 32 * 1024;
    public static String loggedUsername = "";
    public static List<Pair<String, String>> followers = new ArrayList<>();

    public static void main(String[] args) {
        SocketChannel cl = null;
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
                cl.close();
            } catch (Exception e) {
                System.out.println("SocketChannel opening exception");
                System.err.println(e);
            }
        } catch (Exception e) {
            System.err.println(e);
        }

    }

    public static Boolean handleOperations(String[] args, SocketChannel client) {

        if (args.length < 1) return false;

        if (args[0].equals("register")) {
            try {
                SignInHandler signInHandler = new SignInHandler(args);
            } catch (Exception e) {
                return false;
            }
            return false;
        }

        if (args[0].equals("list") && args.length > 1) {
            if (args[1].equals("followers")) {
                if (loggedUsername.equals("")) {
                    System.out.println("< You must login before do this action");
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("< \tUser\t|\tTag\n");
                    stringBuilder.append("< —------------------------------------\n");
                    for (Pair<String, String> follower : followers)
                        stringBuilder.append("< " + follower.getLeft() + "\t|\t" + follower.getRight() + "\n");
                    System.out.println(stringBuilder.toString());
                }
                return false;
            }
        }

        if (args[0].equals("login")) {
            if (args.length > 1) {
                loggedUsername = args[1];
            }
        }

        if (args[0].equals("logout")) {
            loggedUsername = "";
            followers.clear();
        }

        try {
            String request = String.join(" ", args);
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            if (args[0].equals("exit")) {
                buffer.put("CLOSE".getBytes());
                return true;
            }

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

            if (!loggedUsername.equals("")) {

                // Cycle until penultimate row
                String[] splitResponse = response.split("//");

                for (String str: splitResponse) {
                    String[] splitLine = str.split("/");
                    if (splitLine.length > 1)
                        followers.add(new Pair<>(splitLine[0], splitLine[1]));
                }

                if (splitResponse[splitResponse.length - 1].equals("< " + loggedUsername + " logged in")) {
                    try {
                        System.out.println("Searching notification server");
                        Registry registry = LocateRegistry.getRegistry(5000);
                        String name = "AsyncServer";
                        ServerAsyncInterface server = (ServerAsyncInterface) registry.lookup(name);
                        // registering for callback
                        System.out.println("Registering for callback");
                        NotifyEventInterface callbackObj = new NotifyEventImpl(followers, loggedUsername);
                        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
                        server.registerForCallback(stub);
                    } catch (Exception e) {
                        System.out.println("Client exception " + e.getMessage());
                    }
                }
            } else {
                // TODO: Unregistering for server asynchronous callbacks
            }

        } catch (IOException e) {
            System.err.println(e);
        }

        return false;
    }
}

/*
register marco1 DiffPax$123 swift
login marco1 DiffPax$123
 */