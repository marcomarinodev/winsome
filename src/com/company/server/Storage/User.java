package com.company.server.Storage;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {
    private final String username;
    private String encryptedPassword;
    private final String[] tags;
    private Post posts;

    public User(String username, String password, String[] tags) {
        this.username = username;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            encryptedPassword = hash.toString();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown for incorrect security algorithm");
        }

        this.tags = tags;
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String[] getTags() {
        return tags;
    }

    public Post getPosts() {
        return posts;
    }

}
