package com.company.server;

import com.company.SystemCodes;
import com.company.server.Interfaces.SignInService;
import com.company.server.Storage.Post;
import com.company.server.Storage.User;
import com.company.server.Utils.Pair;
import com.company.server.Utils.PersistentOperator;

import java.net.Socket;
import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SignInServiceImpl implements SignInService {

    // Storage
    private final Map<String, User> storage;
    private final Map<String, Post> posts;
    private final Map<String, Socket> loggedUsers = new ConcurrentHashMap<>();

    public SignInServiceImpl(String mex) {
        System.out.println("Retrieving users from users.json");
        Pair<Map<String,User>, Map<String, Post>> pair = PersistentOperator.persistentRead(
                "users.json",
                "posts.json"
        );
        storage = pair.getLeft();
        posts = pair.getRight();
    }

    @Override
    public String register(String username, String password, ArrayList<String> tags) throws RemoteException {
        System.out.println("Registration Attempt");
        // Preconditions
        if (!checkPasswordSecurity(password)) return SystemCodes.WEAK_PASSWORD;
        if (hasUsernameIncorrectFormat(username)) return SystemCodes.MISSING_USERNAME;
        synchronized (storage) {
            if (storage.containsKey(username)) return SystemCodes.USER_ALREADY_EXISTS;
            System.out.println("User registered successfully");
            addUser(new User(username, password, tags));
        }

        PersistentOperator.persistentWrite(storage, posts, "users.json", "posts.json");

        return SystemCodes.SUCCESS;
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

    public Map<String, User> getStorage() {
        return storage;
    }

    public void addUser(User user) {
        this.storage.put(user.getUsername(), user);
    }

    public void addPost(Post post) { this.posts.put(post.getId(), post); }

    public Map<String, Socket> getLoggedUsers() {
        return loggedUsers;
    }

    public Map<String, Post> getPosts() { return posts; }

    public Post getPost(String id) {return posts.get(id); }

    public int getNewId() { return posts.size() + 1; }

    public void addLoggedUser(String username, Socket socket) {
        this.loggedUsers.put(username, socket);
    }

    public void removeLoggedUser(String username) {
        this.loggedUsers.remove(username);
    }
}
