package com.company.server;

import com.company.StatusCodes;
import com.company.server.interfaces.SignInService;

import java.rmi.RemoteException;

public class RemoteServer implements SignInService {
    @Override
    public StatusCodes register(String username, String password, String[] tags) throws RemoteException {
        if (!checkPasswordSecurity(password)) return StatusCodes.BAD_PASSWORD;
        if (hasUsernameIncorrectFormat(username)) return StatusCodes.BAD_USERNAME;


        return StatusCodes.SUCCESS;
    }

    @Override
    public StatusCodes login(String username, String password) throws RemoteException {
        if (password.isEmpty()) return StatusCodes.BAD_PASSWORD;
        if (hasUsernameIncorrectFormat(username)) return StatusCodes.BAD_USERNAME;


        return StatusCodes.SUCCESS;
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
