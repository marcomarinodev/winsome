package com.company.server;

import com.company.SystemCodes;
import com.company.server.Interfaces.SignInService;
import com.company.server.Storage.User;

import java.rmi.RemoteException;
import java.util.Hashtable;

public class SignInServiceImpl implements SignInService {

    // Storage
    private Hashtable<String, User> storage = new Hashtable<String, User>();

    public SignInServiceImpl(String filename) {
        // TODO: get users from JSON
    }

    @Override
    public String register(String username, String password, String[] tags) throws RemoteException {
        System.out.println("Registration Attempt");
        // Preconditions
        if (!checkPasswordSecurity(password)) return SystemCodes.WEAK_PASSWORD;
        if (hasUsernameIncorrectFormat(username)) return SystemCodes.MISSING_USERNAME;
        if (storage.containsKey(username)) return SystemCodes.USER_ALREADY_EXISTS;

        System.out.println("User registered successfully");
        storage.put(username, new User(username, password, tags));
        return SystemCodes.SUCCESS;
    }

    @Override
    public String login(String username, String password) throws RemoteException {
        if (password.isEmpty()) return SystemCodes.MISSING_PASSWORD;
        if (hasUsernameIncorrectFormat(username)) return SystemCodes.MISSING_USERNAME;


        return SystemCodes.SUCCESS;
    }

    @Override
    public void logout(String username) throws RemoteException {

    }

    private boolean hasUsernameIncorrectFormat(String username) {
        return username.length() < 4;
    }

    private boolean checkPasswordSecurity(String password) {
        return passwordSecurityScoreTest(password) > 6;
    }

    private int passwordSecurityScoreTest(String password) {
        //total score of password
        int score = 0;

        if( password.length() < 8 )
            return 0;
        else if( password.length() >= 10 )
            score += 2;
        else
            score += 1;

        // if it contains one digit, add 2 to total score
        if(password.matches("(?=.*[0-9]).*") )
            score += 2;

        // if it contains one lower case letter, add 2 to total score
        if(password.matches("(?=.*[a-z]).*") )
            score += 2;

        // if it contains one upper case letter, add 2 to total score
        if(password.matches("(?=.*[A-Z]).*") )
            score += 2;

        // if it contains one special character, add 2 to total score
        if(password.matches("(?=.*[~!@#$%^&*()_-]).*") )
            score += 2;

        return score;
    }
}
