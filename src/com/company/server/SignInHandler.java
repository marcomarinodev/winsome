package com.company.server;

import com.company.SystemCodes;
import com.company.client.ClientMain;
import com.company.server.Interfaces.SignInService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class SignInHandler {
    public String operation = "";
    public String username = "";
    public String password = "";
    public String tags = "";

    public SignInHandler(String[] args) throws Exception {
        // Preconditions
        if (!checkArgs(args)) {
            throw new Exception();
        }
        System.out.println("Arguments are correct");

        try {
            // Get a reference of the registry
            Registry registry = LocateRegistry.getRegistry(ClientMain.port);
            // Get a reference of the SignInService
            SignInService signInService = (SignInService) registry.lookup(ClientMain.serviceName);
            // Invoke login/logout/register method based on operation
            if (operation.equals("register")) {
                String result = signInService.register(username, password, tags.split(" ", -1));
                SystemCodes.printOperationResult(result, "registration");
            } else if (operation.equals("login")) {
                signInService.login(username, password);
            } else {
                signInService.logout(username);
            }

        } catch (Exception e) {
            System.err.println("RMI Error: " + e.getMessage());
        }
    }

    private Boolean checkArgs(String[] args) {
        this.operation = args[0];
        if (operation == "logout") return true;

        if (args.length < 2) {
            SystemCodes.printOperationResult(SystemCodes.MISSING_USERNAME, "Sign Up/In");
            return false;
        }

        if (args.length < 3) {
            SystemCodes.printOperationResult(SystemCodes.MISSING_PASSWORD, "Sign Up/In");
            return false;
        }

        this.username = args[1];
        this.password = args[2];

        if (args[0].equals("register")) {
            if (args.length < 4) {
                SystemCodes.printOperationResult(SystemCodes.MISSING_TAGS, "Sign Up/In");
                return false;
            }
            this.tags = args[3];
        }

        return true;
    }

}
