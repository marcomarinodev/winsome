import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Post {
    private String id;
    private String title;
    private String content;
    private String author;
    // List of users that has positive vote on this post
    private List<String> positiveVotes;
    // List of users that has negative vote on this post
    private List<String> negativeVotes;
    // List of post ids that rewined this post
    private List<String> rewins;
    private List<Pair<String, String>> comments;
    // Compensation
    private int rewardIterations = 0;
    private List<String> recentPositiveVotes;
    private List<String> recentNegativeVotes;
    // Integer stays for the number of times that the key wrote comments on this post
    private ConcurrentHashMap<String, Integer> recentComments;

    public Post(int id, String title, String content, String author) {
        this.id = String.valueOf(id);
        this.title = title;
        this.content = content;
        this.author = author;
        positiveVotes = new ArrayList<>();
        negativeVotes = new ArrayList<>();
        rewins = new ArrayList<>();
        comments = new ArrayList<>();
        recentPositiveVotes = new ArrayList<>();
        recentNegativeVotes = new ArrayList<>();
        recentComments = new ConcurrentHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public int getPositiveVotesCount() {
        return positiveVotes.size();
    }

    public int getNegativeVotesCount() {
        return negativeVotes.size();
    }

    public void addPositiveVote(String username) {
        positiveVotes.add(username);
        addRecentPositiveVote(username);
    }

    public void removePositiveVote(String username) {
        positiveVotes.remove(username);
    }

    public void addNegativeVote(String username) {
        negativeVotes.add(username);
        addRecentNegativeVote(username);
    }

    public void removeNegativeVote(String username) {
        negativeVotes.remove(username);
    }

    public String getComments() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Pair<String, String> comment: comments)
            stringBuilder.append("\t" + comment.getLeft() + ": \"" + comment.getRight() + "\"\n");

        return stringBuilder.toString();
    }

    public boolean userAlreadyVoted(String username) {
        return positiveVotes.contains(username) || negativeVotes.contains(username);
    }

    public List<String> getRewins() {
        return rewins;
    }

    public void addRewin(String postId) {
        rewins.add(postId);
    }

    public void removeRewin(String postId) {
        rewins.remove(postId);
    }

    public void addComment(String author, String content) {
        comments.add(new Pair<>(author, content));
        addRecentComment(author);
    }

    public int getRewardIterations() {
        return rewardIterations;
    }

    public void addIteration() {
        this.rewardIterations++;
    }

    public List<String> getRecentPositiveVotes() {
        return recentPositiveVotes;
    }

    private void addRecentPositiveVote(String username) {
        this.recentPositiveVotes.add(username);
    }

    public void clearRecentPositiveVotes() {
        this.recentPositiveVotes.clear();
    }

    public List<String> getRecentNegativeVotes() {
        return recentNegativeVotes;
    }

    private void addRecentNegativeVote(String username) {
        this.recentNegativeVotes.add(username);
    }

    public void clearRecentNegativeVotes() {
        this.recentNegativeVotes.clear();
    }

    public ConcurrentHashMap<String, Integer> getRecentComments() {
        return recentComments;
    }

    private void addRecentComment(String username) {
        int nUserCommented = 0;
        for (Pair<String, String> comment : comments) {
            if (comment.getLeft().equals(username))
                nUserCommented++;
        }
        this.recentComments.put(username, nUserCommented);
    }

    public void clearRecentComments() {
        this.recentComments.clear();
    }
}
