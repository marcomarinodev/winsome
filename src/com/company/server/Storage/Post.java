package com.company.server.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private ConcurrentHashMap<String, String> comments;

    public Post(int id, String title, String content, String author) {
        this.id = String.valueOf(id);
        this.title = title;
        this.content = content;
        this.author = author;
        positiveVotes = new ArrayList<>();
        negativeVotes = new ArrayList<>();
        rewins = new ArrayList<>();
        comments = new ConcurrentHashMap<String, String>();
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
    }

    public void removePositiveVote(String username) {
        positiveVotes.remove(username);
    }

    public void addNegativeVote(String username) {
        negativeVotes.add(username);
    }

    public void removeNegativeVote(String username) {
        negativeVotes.remove(username);
    }

    public String getComments() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> comment: comments.entrySet())
            stringBuilder.append("\t" + comment.getKey() + ": \"" + comment.getValue() + "\"\n");

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
}
