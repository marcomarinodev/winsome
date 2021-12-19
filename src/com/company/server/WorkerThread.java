package com.company.server;

import com.company.server.Storage.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class WorkerThread implements Runnable {

    private Socket socket;
    private String request;
    private SignInServiceImpl signInService;

    WorkerThread(Socket socket, SignInServiceImpl signInService) {
        this.socket = socket;

        System.out.println(socket.getInetAddress().getHostAddress());

        Scanner in = null;
        try {
            in = new Scanner(socket.getInputStream());
            request = in.nextLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }

        this.signInService = signInService;
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
        System.out.println("Worker is working");

        String[] splitReq = getOperation();
        String operation = splitReq[0];

        if (operation.equals("login")) {
            performLogin(splitReq);
        }

        System.out.println("Worker finished");
    }

    private void performLogin(String[] splitReq) {

        if (signInService.getLoggedUsers().containsKey(splitReq[1])) {

        }

        // I am sure that the client has sent a correct format request
        if (signInService.getStorage().containsKey(splitReq[1])) {
            String password = signInService.getStorage().get(splitReq[1]).getEncryptedPassword();
            if (User.hashEncrypt(splitReq[2]).equals(password)) {
                System.out.println("User accepted");
                signInService.addLoggedUser(splitReq[1], socket);
            } else {
                System.out.println("Attempted password: " + User.hashEncrypt(splitReq[2]));
                System.out.println("Truly password: " + password);
                System.out.println("Wrong Password");
            }
        } else {
            System.out.println("User does not exists");
        }
    }

    private String[] getOperation() {
        return request.split(" ");
    }
}
