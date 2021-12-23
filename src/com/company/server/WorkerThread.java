package com.company.server;

import com.company.server.Storage.User;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import static com.company.server.Utils.NIOHelper.readRequest;
import static com.company.server.Utils.NIOHelper.writeResponse;

public class WorkerThread implements Runnable {

    private final SelectionKey key;
    private final ByteBuffer buffer;
    private final SocketChannel client;
    private final String request;
    private final SignInServiceImpl signInService;

    WorkerThread(SelectionKey key, SignInServiceImpl signInService) {
        this.key = key;
        this.client = (SocketChannel) key.channel();
        this.buffer = (ByteBuffer) key.attachment();
        this.signInService = signInService;
        request = readRequest(key, this.buffer);
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
            } else if (operation.equals("list")) {
                performListOperation(splitReq);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // I need to say to selector to re-listen to this socket
        buffer.clear();
        ServerMain.readingKeys.add(key);
    }

    private void performListOperation(String[] splitReq) throws IOException {

    }

    private void performLogin(String[] splitReq) throws IOException {

        if (splitReq.length < 3) {
            writeResponse("< Missing Credentials", client);
            return;
        }

        if (signInService.getLoggedUsers().containsKey(splitReq[1])) {
            if (signInService.getLoggedUsers().get(splitReq[1]) == client.socket()) {
                writeResponse("< You're already logged in", client);
            } else {
                writeResponse("< There is a logged in user, you must log out from it", client);
            }
            return;
        }

        // I am sure that the client has sent a correct format request
        if (signInService.getStorage().containsKey(splitReq[1])) {

            String password = signInService.getStorage().get(splitReq[1]).getEncryptedPassword();

            if (User.hashEncrypt(splitReq[2]).equals(password)) {
                System.out.println("User accepted");
                signInService.addLoggedUser(splitReq[1], client.socket());
                writeResponse("< " + splitReq[1] + " logged in", client);
            } else {
                System.out.println("Wrong Password");
                writeResponse("< Wrong Password", client);
            }
        } else {
            System.out.println("User does not exists");
            writeResponse("< Error " + splitReq[1] + " does not exists", client);
        }
    }

    private void performLogout() throws IOException {
        if (signInService.getLoggedUsers().containsValue(client.socket())) {
            String key = getKey(signInService.getLoggedUsers(), client.socket());
            signInService.removeLoggedUser(key);
            writeResponse("< " + key + " logged out", client);
        } else {
            System.out.println("No user logged in");
            writeResponse("< You're not logged in, please log in", client);
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
