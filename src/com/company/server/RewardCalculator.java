package com.company.server;

import com.company.server.Storage.Post;
import com.company.server.Storage.User;
import com.company.server.Utils.PersistentOperator;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

public class RewardCalculator implements Runnable {

    private int interval;
    private SignInServiceImpl signInService;

    public RewardCalculator(int interval, SignInServiceImpl signInService) {
        this.interval = interval;
        this.signInService = signInService;
    }

    @Override
    public void run() {
        try (DatagramSocket multiSocket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName("239.255.32.32");
            if (!group.isMulticastAddress()) {
                throw new IllegalArgumentException("Invalid multicast address: "
                + group.getHostAddress());
            }
            while (true) {
                try {
                    Thread.sleep(interval * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // For every post, calculate the compensation and update
                synchronized (signInService.getPosts()) {
                    for (Map.Entry<String, Post> postEntry : signInService.getPosts().entrySet()) {
                        int newPeopleLikesLogArg = 0;
                        float newPeopleCommentingLogArg = 0;
                        Post post = postEntry.getValue();
                        String author = post.getAuthor();
                        User authorUser = signInService.getStorage().get(author);
                        Integer nIterations = post.getRewardIterations() + 1;
                        double revenue = 0;

                        // New People Likes Log Arg
                        int numPositiveVotes = post.getRecentPositiveVotes().size();
                        int numNegativeVotes = post.getRecentNegativeVotes().size();
                        newPeopleLikesLogArg = Math.max(numPositiveVotes - numNegativeVotes, 0);

                        for (Map.Entry<String, Integer> recentCommentEntry : post.getRecentComments().entrySet()) {
                            Integer cp = recentCommentEntry.getValue();
                            newPeopleCommentingLogArg += (2 / (1 + Math.exp(1 - cp)));
                        }

                        revenue = (Math.log(newPeopleLikesLogArg + 1) + Math.log(newPeopleCommentingLogArg + 1)) / nIterations;

                        // Send via udp multicast the compensation action
                        byte[] content = "\n< Wallet updated!".getBytes();
                        DatagramPacket packet = new DatagramPacket(content, content.length, group, 33333);

                        multiSocket.send(packet);

                        authorUser.setTotalCompensation(authorUser.getTotalCompensation() + revenue);

                        // You need to empty recent arrays from post
                        post.clearRecentPositiveVotes();
                        post.clearRecentNegativeVotes();
                        post.clearRecentComments();

                        // Add iteration
                        post.addIteration();
                    }
                }

                PersistentOperator.persistentWrite(
                        signInService.getStorage(),
                        signInService.getPosts(),
                        "users.json",
                        "posts.json");

                System.out.println("Compensations were computed!");
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}