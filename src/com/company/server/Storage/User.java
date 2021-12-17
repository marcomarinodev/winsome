package com.company.server.Storage;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {
    private final String username;
    private String encryptedPassword;
    private final String[] tags;

    public User(String username, String password, String tags) {
        this.username = username;

        try {
            encryptedPassword = toHexString(getSHA(password));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown for incorrect security algorithm");
        }

        this.tags = tags.split(" ", -1);
    }

    private byte[] getSHA(String input) throws NoSuchAlgorithmException {
        // Message Digest instance for hash using SHA512
        MessageDigest md = MessageDigest.getInstance("S512");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHexString(byte[] hash) {
        // Convert byte array of hash into digest
        BigInteger number = new BigInteger(1, hash);

        // Convert the digest into hex value
        StringBuilder hexString = new StringBuilder(number.toString(16));

        // Pad with leading zeros
        while (hexString.length() > 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
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
}
