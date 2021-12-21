package com.company.server;

import com.company.server.Storage.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class WorkerThread implements Runnable {

    private SelectionKey key;
    private ByteBuffer buffer;
    private SocketChannel client;
    private String request;
    private SignInServiceImpl signInService;

    WorkerThread(SelectionKey key, SignInServiceImpl signInService, ByteBuffer byteBuffer) {
        this.key = key;
        this.client = (SocketChannel) key.channel();
        this.buffer = byteBuffer;
        this.signInService = signInService;
        ServerMain.readingKeys.put(key, 0);
        readRequest(key, byteBuffer);
    }

    @Override
    public void run() {
        System.out.println("Resolving: " + client.socket());

        try {
            String[] splitReq = getOperation();
            String operation = splitReq[0];
            System.out.println("Current Request: " + request);
            if (operation.equals("login")) {
                System.out.println("Login request");
                performLogin(splitReq);
            } else if (operation.equals("logout")) {
                System.out.println("Logout request");
                performLogout();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // I need to say to selector to re-listen to this socket
        buffer.clear();
        ServerMain.readingKeys.remove(key);
    }

    private void readRequest(SelectionKey key, ByteBuffer byteBuffer) {
        try {
            SocketChannel client = (SocketChannel) key.channel();
            byteBuffer.clear();
            client.read(byteBuffer);
            byteBuffer.flip();
            int receivedLength = byteBuffer.getInt();
            byte[] receivedBytes = new byte[receivedLength];
            byteBuffer.get(receivedBytes);
            request = new String(receivedBytes);

        } catch (IOException e) {
            System.out.println("ERROR");
        }
    }

    private void writeResponse(String message) throws IOException {
        ByteBuffer newBuffer = ByteBuffer.allocate(32 * 1024);
        byte[] mex = message.getBytes();
        newBuffer.clear();
        newBuffer.putInt(mex.length);
        newBuffer.put(mex);
        newBuffer.flip();
        client.write(newBuffer);
    }

    private void performLogin(String[] splitReq) throws IOException {

        if (splitReq.length < 3) {
            writeResponse("Missing Credentials");
            return;
        }

        if (signInService.getLoggedUsers().containsKey(splitReq[1])) {
            if (signInService.getLoggedUsers().get(splitReq[1]) == client.socket()) {
                writeResponse("You're already logged in");
            } else {
                writeResponse("There is a logged in user, you must log out from it");
            }
            return;
        }

        // I am sure that the client has sent a correct format request
        if (signInService.getStorage().containsKey(splitReq[1])) {

            String password = signInService.getStorage().get(splitReq[1]).getEncryptedPassword();

            if (User.hashEncrypt(splitReq[2]).equals(password)) {
                System.out.println("User accepted");
                signInService.addLoggedUser(splitReq[1], client.socket());
                writeResponse(splitReq[1] + " logged in");
            } else {
                System.out.println("Wrong Password");
                writeResponse("Wrong Password");
            }
        } else {
            System.out.println("User does not exists");
            writeResponse("Error " + splitReq[1] + " does not exists");
        }
    }

    private void performLogout() throws IOException {
        if (signInService.getLoggedUsers().containsValue(client.socket())) {
            String key = getKey(signInService.getLoggedUsers(), client.socket());
            signInService.removeLoggedUser(key);
            writeResponse(key + " logged out");
        } else {
            System.out.println("No user logged in");
            writeResponse("You're not logged in, please log in");
        }
    }

    private <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String[] getOperation() {
        return request.split(" ");
    }
}
