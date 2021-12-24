package com.company.client;

import com.company.server.SignInHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientMain {

    public static int port = 12120;
    public static String serviceName = "RMISignIn";
    public static final String hostname = "localhost";
    public static final int bufferSize = 32 * 1024;
    public static String loggedUsername = "";

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
                System.out.println("> list followers it is not available yet");
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
                if (response.equals("< " + loggedUsername + " logged in")) {
                    // TODO: Registering for server asynchronous callbacks
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