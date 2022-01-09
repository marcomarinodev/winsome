import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RewardCalculator implements Runnable {

    private final int interval;
    private final StorageService storageService;
    private final double authorPercentage;
    private final String multicastAddress;
    private final int multicastPort;

    public RewardCalculator(int interval, StorageService storageService, double authorPercentage,
                            String multicastAddress, int multicastPort) {
        this.interval = interval;
        this.storageService = storageService;
        this.authorPercentage = authorPercentage;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    @Override
    public void run() {
        try (DatagramSocket multiSocket = new DatagramSocket()) {
            System.out.println("MULTICAST: " + multicastAddress);
            System.out.println("MULTIPORT: " + multicastPort);
            InetAddress group = InetAddress.getByName(multicastAddress);
            if (!group.isMulticastAddress()) {
                throw new IllegalArgumentException("(RewardCalculator) Invalid multicast address: "
                + group.getHostAddress());
            }
            while (true) {

                System.out.println("Computing rewards...");

                try {
                    Thread.sleep(interval * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // For every post, calculate the compensation and update
                synchronized (storageService) {
                    for (Map.Entry<String, Post> postEntry : storageService.posts.entrySet()) {
                        int newPeopleLikesLogArg = 0;
                        float newPeopleCommentingLogArg = 0;
                        Post post = postEntry.getValue();
                        String author = post.getAuthor();
                        User authorUser = storageService.storage.get(author);
                        int nIterations = post.getRewardIterations() + 1;
                        double revenue;
                        Set<User> curators = new HashSet<>();

                        // New People Likes Log Arg
                        int numPositiveVotes = post.getRecentPositiveVotes().size();
                        int numNegativeVotes = post.getRecentNegativeVotes().size();
                        newPeopleLikesLogArg = Math.max(numPositiveVotes - numNegativeVotes, 0);

                        for (Map.Entry<String, Integer> recentCommentEntry : post.getRecentComments().entrySet()) {
                            Integer cp = recentCommentEntry.getValue();
                            newPeopleCommentingLogArg += (2 / (1 + Math.exp(1 - cp)));
                            curators.add(storageService.storage.get(recentCommentEntry.getKey()));
                        }

                        revenue = (Math.log(newPeopleLikesLogArg + 1) + Math.log(newPeopleCommentingLogArg + 1)) / nIterations;

                        // we have to split: 70% of revenues to author and other 30% to curators
                        double authorEarning = revenue * (authorPercentage/100);
                        double curatorsEarning = revenue * ((100 - authorPercentage)/100);

                        if (authorEarning > 0) {
                            authorUser.setTotalCompensation(authorUser.getTotalCompensation() + authorEarning);
                            authorUser.addTransaction(new Pair<>(String.valueOf(authorEarning),
                                    new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").format(new Date())));
                        }

                        for (String curator: post.getRecentPositiveVotes()) {
                            curators.add(storageService.storage.get(curator));
                        }

                        for (User curator: curators) {
                            double gain = (curatorsEarning/curators.size());
                            if (gain > 0) {
                                curator.setTotalCompensation(curator.getTotalCompensation() +
                                        gain);
                                curator.addTransaction(new Pair<>(String.valueOf(gain),
                                        new SimpleDateFormat("dd-MM-yyyy hh:mm:ss").format(new Date())));
                            }
                        }

                        // You need to empty recent arrays from post
                        post.clearRecentPositiveVotes();
                        post.clearRecentNegativeVotes();
                        post.clearRecentComments();

                        // Add iteration
                        post.addIteration();

                    }
                }

                PersistentOperator.persistentWrite(
                        storageService.storage,
                        storageService.posts,
                        "users.json",
                        "posts.json");

                // Send via udp multicast the compensation action
                byte[] content = "Wallet updated".getBytes();
                DatagramPacket packet = new DatagramPacket(content, content.length, group, multicastPort);

                multiSocket.send(packet);
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
