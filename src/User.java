import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class User {
    private final String username;
    private final String encryptedPassword;
    private final ArrayList<String> tags;
    private ArrayList<String> postIds = new ArrayList<>();
    private ArrayList<String> followers = new ArrayList<>();
    private ArrayList<String> followings = new ArrayList<>();
    private double totalCompensation = 0;
    private ArrayList<Pair<String, String>> transactions = new ArrayList<>();

    public User(String username, String password, ArrayList<String> tags) {
        this.username = username;
        this.encryptedPassword = hashEncrypt(password);
        this.tags = tags;
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
            hexString.insert(0, '0');

        return hexString.toString();
    }

    @Override
    public String toString() {
        return getUsername() + "\t\t" + " | " + tagsToString();
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public String tagsToString() {
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;

        for (String tag : getTags()) {
            stringBuilder.append(tag).append(" ");
            if (counter < getTags().size() - 1)
                stringBuilder.append(", ");
            counter++;
        }

        return stringBuilder.toString();
    }

    public ArrayList<String> getPostIds() {
        return postIds;
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    public ArrayList<String> getFollowings() {
        return followings;
    }

    public void setPostIds(ArrayList<String> postIds) {
        this.postIds = postIds;
    }

    public boolean existsFollower(String follower) {
        return this.followers.contains(follower);
    }

    public boolean addFollower(String follower) {
        if (existsFollower(follower)) return false;
        this.followers.add(follower);
        return true;
    }

    public boolean removeFollower(String follower) {
        if (this.followers.contains(follower)) {
            this.followers.remove(follower);
            return true;
        }
        return false;
    }

    public boolean existsFollowing(String following) {
        return this.followings.contains(following);
    }

    public boolean addFollowing(String following) {
        if (existsFollowing(following)) return false;
        this.followings.add(following);
        return true;
    }

    public boolean removeFollowing(String following) {
        if (this.followings.contains(following)) {
            this.followings.remove(following);
            return true;
        }
        return false;
    }

    public boolean addPost(String postId) {
        return postIds.add(postId);
    }

    public boolean removePost(String postId) {
        return postIds.remove(postId);
    }

    public double getTotalCompensation() {
        return totalCompensation;
    }

    public void setTotalCompensation(double totalCompensation) {
        if (totalCompensation >= 0) this.totalCompensation = totalCompensation;
        else this.totalCompensation = 0;
    }

    public void addTransaction(Pair<String, String> transaction) {
        transactions.add(transaction);
    }

    public ArrayList<Pair<String, String>> getTransactions() {
        return transactions;
    }
}
