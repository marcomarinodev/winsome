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
    private ConcurrentHashMap<String, String> comments;

    public Post(int id, String title, String content, String author) {
        this.id = String.valueOf(id);
        this.title = title;
        this.content = content;
        this.author = author;
        positiveVotes = new ArrayList<>();
        negativeVotes = new ArrayList<>();
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

    public String getComments() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> comment: comments.entrySet())
            stringBuilder.append("\t" + comment.getKey() + ": \"" + comment.getValue() + "\"\n");

        return stringBuilder.toString();
    }
}
