package com.company.server.Storage;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class User {
    private final String username;
    private String encryptedPassword;
    private final String[] tags;
    private String[] postIds;

    public User(String username, String password, String[] tags) {
        this.username = username;
        this.encryptedPassword = hashEncrypt(password);
        this.tags = tags;
        this.postIds = new String[0];
    }

    public static String hashEncrypt(String password) {
        String encryptedPassword = "";
        try {
            encryptedPassword = toHexString(getSHA(password));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown for incorrect security algorithm");
        }

        return encryptedPassword;
    }

    public static byte[] getSHA(String input) throws NoSuchAlgorithmException
    {
        // Static getInstance method is called with hashing SHA
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // digest() method called
        // to calculate message digest of an input
        // and return array of byte
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHexString(byte[] hash)
    {
        // Convert byte array into signum representation
        BigInteger number = new BigInteger(1, hash);

        // Convert message digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() < 32)
        {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", encryptedPassword='" + encryptedPassword + '\'' +
                ", tags=" + Arrays.toString(tags) +
                ", postIds=" + Arrays.toString(postIds) +
                '}';
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

    public String[] getPostIds() {
        return postIds;
    }

}
