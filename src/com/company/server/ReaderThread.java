package com.company.server;

import com.company.server.Storage.User;
import com.company.server.Utils.NIOHelper;
import com.company.server.Utils.PersistentOperator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;

public class ReaderThread implements Runnable {

    private final SelectionKey key;
    private final ByteBuffer buffer = ByteBuffer.allocate(32*1024);
    private final SocketChannel client;
    private final String request;
    private final SignInServiceImpl signInService;
    private Selector selector;

    ReaderThread(SelectionKey key, SignInServiceImpl signInService, Selector selector) {
        this.key = key;
        this.client = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(32 * 1024);
        key.attach(byteBuffer);
        this.signInService = signInService;
        this.selector = selector;
        request = NIOHelper.readRequest(key, (ByteBuffer) key.attachment());
    }

    @Override
    public void run() {
        System.out.println("Resolving: " + client.socket());
        String result = "";

        try {
            String[] splitReq = getOperation();
            String operation = splitReq[0];
            System.out.println("Current Request: " + request);
            if (operation.equals("login")) {
                System.out.println("Login request");
                result = performLogin(splitReq);
            } else if (operation.equals("logout")) {
                System.out.println("Logout request");
                result = performLogout();
            } else if (operation.equals("list")) {
                System.out.println("List request");
                result = performListOperation(splitReq);
            } else if (operation.equals("follow")) {
                System.out.println("Follow request");
                result = performFollow(splitReq);
            } else if (operation.equals("unfollow")) {
                System.out.println("Unfollow request");
                result = performUnfollow(splitReq);
            } else {
                result = "< " + request + "operation is not supported";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // I need to say to selector to re-listen to this socket
        key.attach(result);

        try {
            key.channel().register(selector, SelectionKey.OP_WRITE, key.attachment());
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
        selector.wakeup();
        System.out.println("End ReaderThread");
    }

    private String performFollow(String[] splitReq) {
        if (checkListArgsCount(splitReq)) return "< list has not enough parameters";
        if (isUserLogged()) return "< You must login in order to do this operation";
        if (!existsUser(splitReq[1])) return "< " + splitReq[1] + " does not exist";

        String toFollowUser = splitReq[1];
        String loggedUser = getKey(signInService.getLoggedUsers(), client.socket());
        User loggedUserObj = signInService.getStorage().get(loggedUser);
        User toFollowUserObj = signInService.getStorage().get(toFollowUser);

        // Add following to loggedUserObj
        loggedUserObj.addFollowing(toFollowUser);
        // Add follower to toFollowUser
        toFollowUserObj.addFollower(loggedUser);

        PersistentOperator.persistentWrite(
                signInService.getStorage(),
                signInService.getPosts(),
                "users.json",
                "posts.json");

        return "< now you follow " + toFollowUser;
    }

    private String performUnfollow(String[] splitReq) {
        return "";
    }

    private String performListOperation(String[] splitReq) throws IOException {
        if (checkListArgsCount(splitReq)) return "< list has not enough parameters";
        if (isUserLogged()) return "< You must login in order to do this operation";

        String loggedUser = getKey(signInService.getLoggedUsers(), client.socket());
        StringBuilder stringBuilder = new StringBuilder();
        String topic = splitReq[1];
        boolean notRecognizedTopic = false;

        switch (topic) {
            case "users" -> listUsers(loggedUser, stringBuilder);
            case "followers" -> listFollowers(loggedUser, stringBuilder);
            case "following" -> listFollowing(loggedUser, stringBuilder);
            default -> notRecognizedTopic = true;
        }

        if (notRecognizedTopic) return "< " + topic + " is not listable";

        return stringBuilder.toString();
    }

    private boolean isUserLogged() {
        return !signInService.getLoggedUsers().containsValue(client.socket());
    }

    private void listFollowers(String loggedUser, StringBuilder stringBuilder) {
        // TODO: list all logged user's followers
    }

    private void listFollowing(String loggedUser, StringBuilder stringBuilder) {
        // TODO: following list of the logged user
    }

    private void listUsers(String loggedUser, StringBuilder stringBuilder) {
        stringBuilder.append("< \tUser\t|\tTag\n");
        stringBuilder.append("< —------------------------------------\n");
        ArrayList<String> loggedUserTags = signInService.getStorage().get(loggedUser).getTags();

        for (Map.Entry<String, User> user: signInService.getStorage().entrySet()) {
            // do not print the current user
            if (user.getValue().getUsername().equals(loggedUser)) continue;

            // check if user and loggedUser have tags in common
            boolean contains = false;
            for (String loggedUserTag: loggedUserTags) {
                if (user.getValue().getTags().contains(loggedUserTag)) {
                    contains = true;
                    break;
                }
            }

            if (contains) stringBuilder.append("< " + user.getValue().toString() + "\n");
        }

        stringBuilder.append("< \t...");
    }

    private boolean checkListArgsCount(String[] splitReq) { return splitReq.length < 2; }

    private boolean existsUser(String username) {
        return signInService.getStorage().containsKey(username);
    }

    private String performLogin(String[] splitReq) throws IOException {

        if (splitReq.length < 3) {
            return "< Missing Credentials";
        }

        // Check if exists a user entry inside logged user
        if (signInService.getLoggedUsers().containsKey(splitReq[1])) {
            if (signInService.getLoggedUsers().get(splitReq[1]) == client.socket()) {
                return "< You're already logged in";
            } else {
                return "< There is a logged in user, you must log out from it";
            }
        } else {
            if (signInService.getLoggedUsers().containsValue(client.socket())) {
                return "< You're already logged in";
            }
        }

        // I am sure that the client has sent a correct format request
        if (signInService.getStorage().containsKey(splitReq[1])) {

            String password = signInService.getStorage().get(splitReq[1]).getEncryptedPassword();

            if (User.hashEncrypt(splitReq[2]).equals(password)) {
                System.out.println("User accepted");
                signInService.addLoggedUser(splitReq[1], client.socket());
                return "< " + splitReq[1] + " logged in";
            } else {
                System.out.println("Wrong Password");
                return "< Wrong Password";
            }
        } else {
            System.out.println("User does not exists");
            return "< Error " + splitReq[1] + " does not exists";
        }
    }

    private String performLogout() throws IOException {
        if (signInService.getLoggedUsers().containsValue(client.socket())) {
            String key = getKey(signInService.getLoggedUsers(), client.socket());
            signInService.removeLoggedUser(key);
            return "< " + key + " logged out";
        } else {
            System.out.println("No user logged in");
            return "< You're not logged in, please log in";
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
