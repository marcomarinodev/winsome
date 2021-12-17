package com.company.client;

import com.company.server.SignInHandler;

public class ClientMain {

    public static int port = 12120;
    public static String serviceName = "RMISignIn";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Not enough parameters");
            System.exit(1);
        }

        System.out.println(args[0]);
        handleOperations(args);

    }

    public static void handleOperations(String[] args) {
        if (args[0].equals("login" ) || args[0].equals("register")) {
            try {
                SignInHandler signInHandler = new SignInHandler(args);
            } catch (Exception e) {
                System.exit(1);
            }
        }
    }

}
