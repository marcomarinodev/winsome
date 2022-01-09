import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.*;

public class ReaderThread implements Runnable {

    // incoming client key
    private final SelectionKey key;
    // incoming client channel
    private final SocketChannel client;
    // client's request
    private final String request;
    // storage service to get users and posts and basic
    // operations on them
    private final StorageService storageService;
    // given selector (to awake)
    private final Selector selector;
    // send async notifications
    private final ServerAsyncImpl asyncServer;
    // current logged user
    private final String loggedUser;

    /**
     * @param key incoming client key
     * @param storageService storage service to get users and posts and basic
     *                       and operations on them
     * @param selector given selector
     * @param asyncServer async notifications server
     */
    ReaderThread(SelectionKey key, StorageService storageService, Selector selector, ServerAsyncImpl asyncServer) {
        this.key = key;
        this.client = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(32 * 1024);
        key.attach(byteBuffer);
        this.storageService = storageService;
        this.selector = selector;
        request = NIOHelper.readRequest(key, (ByteBuffer) key.attachment());
        this.asyncServer = asyncServer;
        loggedUser = getKey(storageService.loggedUsers, client.socket());
    }

    @Override
    public void run() {
        System.out.println("Resolving: " + client.socket());
        String result = "";

        try {
            String[] splitReq = getOperation();
            String operation = splitReq[0];
            System.out.println("Current Request: " + request);
            switch (operation) {
                case "login" -> {
                    System.out.println("Login request");
                    result = performLogin(splitReq);
                }
                case "logout" -> {
                    System.out.println("Logout request");
                    result = performLogout();
                }
                case "list" -> {
                    System.out.println("List request");
                    result = performListOperation(splitReq);
                }
                case "follow" -> {
                    System.out.println("Follow request");
                    result = performFollow(splitReq);
                }
                case "unfollow" -> {
                    System.out.println("Unfollow request");
                    result = performUnfollow(splitReq);
                }
                case "post" -> {
                    System.out.println("Create post request");
                    result = performAddPost(request);
                }
                case "show" -> {
                    System.out.println("Show post request");
                    result = performShowOperation(splitReq);
                }
                case "rate" -> {
                    System.out.println("Rate post request");
                    result = performRatePost(splitReq);
                }
                case "blog" -> {
                    System.out.println("View Blog request");
                    result = performBlog();
                }
                case "delete" -> {
                    System.out.println("Delete post request");
                    result = performDelete(splitReq);
                }
                case "rewin" -> {
                    System.out.println("Rewin post request");
                    result = performRewin(splitReq);
                }
                case "comment" -> {
                    System.out.println("Comment post request");
                    result = performComment(request);
                }
                case "wallet" -> {
                    System.out.println("Wallet request");
                    result = performWalletOperation(splitReq);
                }
                case "exit" -> {
                    System.out.println("Exit request");
                    synchronized (storageService.loggedUsers) {
                        if (isUserLogged()) {
                            String key = getKey(storageService.loggedUsers, client.socket());
                            storageService.removeLoggedUser(key);
                        }
                    }
                    result = "< Goodbye!";
                }
                default -> result = "< " + request + "operation is not supported";
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

    private String performWalletOperation(String[] splitReq) {
        if (!isUserLogged()) return "< You must login to do this operation";
        User loggedUserObj = storageService.storage.get(loggedUser);

        if (splitReq.length == 1) {
            StringBuilder stringBuilder = new StringBuilder("< you have " + loggedUserObj.getTotalCompensation() + " wincoins\n");

            synchronized (storageService.storage) {
                for (Pair<String, String> transaction : loggedUserObj.getTransactions()) {
                    stringBuilder.append("< coins: " + transaction.getLeft()
                            + "; timestamp: " + transaction.getRight() + "\n");
                }
            }

            return stringBuilder.toString();
        }

        // Client requested wallet btc
        String randomURL = "https://www.random.org/decimal-fractions/?num=1&dec=5&col=2&format=plain&rnd=new";
        try {
            // set default encoding
            String encoding = "ISO-8859-1";
            URL u = new URL(randomURL);
            URLConnection uc = u.openConnection();
            String contentType = uc.getContentType();
            System.out.println("contenttype" + contentType);
            int encodingStart = contentType.indexOf("charset=");
            if (encodingStart != -1) {
                encoding = contentType.substring(encodingStart + 8);
            }
            System.out.println("encoding"+encoding);
            InputStream in = new BufferedInputStream(uc.getInputStream());
            Reader r = new InputStreamReader(in, encoding);
            int c;
            StringBuilder stringBuilder = new StringBuilder();
            while ((c = r.read()) != -1) {
                stringBuilder.append((char) c);
            }
            r.close();
            double btcValue = Double.parseDouble(stringBuilder.toString());
            return "< " + btcValue * loggedUserObj.getTotalCompensation() + " BTC";
        } catch (MalformedURLException ex) {
            System.err.println(randomURL + " is not a parseable URL");
        } catch (UnsupportedEncodingException ex) {
            System.err.println(
                    "Server sent an encoding Java does not support: " +
                        ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex);
        }

        return "";
    }

    private String performComment(String request) {

        String[] splitReq = request.split(" ");
        String comment = NIOHelper.removeLastChar(request.split(" \"")[1]);

        if (comment.length() == 0 || comment.length() > 20) return "< Comment must have 1-20 characters";

        if (!isUserLogged()) return "< You must login to do this operation";

        synchronized (storageService.posts) {
            // Check if post exists
            Post post = storageService.getPost(splitReq[1]);
            if (post == null) return "< post " + splitReq[1] + " does not exist";

            post.addComment(loggedUser, comment);
        }

        return "< You just commented successfully";
    }

    private String performRewin(String[] splitReq) {
        if (!isUserLogged()) return "< You must login to do this operation";

        synchronized (storageService.posts) {
            // Check if post exists
            Post post = storageService.getPost(splitReq[1]);
            if (post == null) return "< post " + splitReq[1] + " does not exist";
            if (post.getAuthor().equals(loggedUser)) return "< You cannot rewin your post";

            // Generate rewin post id
            Post rewinPost = new Post(storageService.getNewId(), "(rew) " + post.getTitle(), post.getContent(), loggedUser);

            // Add rewin post id into the original post
            post.addRewin(rewinPost.getId());

            // Add rewin post
            storageService.addPost(rewinPost);
        }

        return "< Succesfully post rewin";
    }

    private String performDelete(String[] splitReq) {
        if (!isUserLogged()) return "< You must login to do this operation";

        synchronized (storageService.posts) {
            // Check if post exists
            Post post = storageService.getPost(splitReq[1]);
            if (post == null) return "< post " + splitReq[1] + " does not exist";

            // At this point post exists
            // Check if the author of this post is the logged user
            // otherwise return error
            if (!post.getAuthor().equals(loggedUser)) return "< You're not the author of this post";


            // Before removing post itself, we have to delete rewins
            for (String rewinPostId: storageService.getPost(post.getId()).getRewins()) {
                // remove rewin with id = rewinPostId
                storageService.posts.remove(rewinPostId);
            }
            storageService.posts.remove(post.getId());
        }

        return "< Post " + splitReq[1] + " successfully deleted";
    }

    private String performBlog() {
        if (!isUserLogged()) return "< You must login to do this operation";
        List<Post> posts = storageService.getPostsOf(loggedUser);
        StringBuilder stringBuilder = new StringBuilder();

        for (Post post: posts) {
            stringBuilder.append("< Title: " + post.getTitle() + "\n");
            stringBuilder.append("< Content: " + post.getContent() + "\n");
            stringBuilder.append("< Votes: " + post.getPositiveVotesCount() + " positives, "
                    + post.getNegativeVotesCount() + " negatives\n");
            stringBuilder.append("< Comments:\n" + post.getComments());
            stringBuilder.append("------------------------\n");
        }

        return stringBuilder.toString();
    }

    private String performShowOperation(String[] splitReq) {
        if (splitReq[1].equals("post")) {
            return performShowPost(splitReq);
        } else if (splitReq[1].equals("feed")) {
            return performShowFeed();
        }
        else return "< " + splitReq[1] + " is not a show option";
    }

    private String performShowFeed() {
        // You must log in
        if (!isUserLogged()) return "< You must login to do this operation";
        List<Post> filteredPosts = new ArrayList<>();
        User loggedUserObj = storageService.storage.get(loggedUser);

        synchronized (storageService.posts) {
            for (String following: loggedUserObj.getFollowings()) {
                filteredPosts.addAll(storageService.getPostsOf(following));
            }
        }

        Collections.shuffle(filteredPosts);
        StringBuilder stringBuilder = new StringBuilder();

        for (Post post: filteredPosts) {
            // id author title
            stringBuilder.append("< " + post.getId() +
                    " " + post.getAuthor() +
                    " " + post.getTitle() + "\n");
        }

        return stringBuilder.toString();
    }

    private String performRatePost(String[] splitReq) {
        // You have to be logged
        if (!isUserLogged()) return "< You must login to do this operation";
        if (splitReq.length < 3) return "< You're missing some show post arguments";

        synchronized (storageService.posts) {
            // Check if post exists
            Post post = storageService.getPost(splitReq[1]);
            if (post == null) return "< post " + splitReq[1] + " does not exist";
            // You have to check if this post is inside loggedUser feed
            // In other words, the post author has to be a following

            if (!storageService.storage.get(loggedUser).existsFollowing(post.getAuthor()))
                return "< This post is not in your feed";
            // Check if you've already voted this post
            if (post.userAlreadyVoted(loggedUser)) return "< you've already voted this post";
            // You cannot rate your post
            if (loggedUser.equals(post.getAuthor()))
                return "< You cannot rate your post";

            // now you can vote
            if (Integer.parseInt(splitReq[2]) > 0) post.addPositiveVote(loggedUser);
            else post.addNegativeVote(loggedUser);
        }

        return "< You've rated post " + splitReq[2];
    }

    private String performShowPost(String[] splitReq) {
        // You don't have to be logged
        // Check if post exists
        Post post = storageService.getPost(splitReq[2]);
        if (post == null) return "< post " + splitReq[2] + " does not exist";
        // Get post object
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("< Title: " + post.getTitle() + "\n");
        stringBuilder.append("< Content: " + post.getContent() + "\n");
        stringBuilder.append("< Votes: " + post.getPositiveVotesCount() + " positives, "
                                + post.getNegativeVotesCount() + " negatives\n");
        stringBuilder.append("< Comments:\n" + post.getComments());

        return stringBuilder.toString();
    }

    private String performAddPost(String request) {
        if (!isUserLogged()) return "< You must login to do this operation";
        String[] titleContent = request.split(" \"");
        if (titleContent.length < 3) return "< You're missing some arguments";

        String title = NIOHelper.removeLastChar(titleContent[1]);
        String content = NIOHelper.removeLastChar(titleContent[2]);

        if (title.length() == 0 || title.length() > 20) return "Title must have 1-20 characters";
        if (content.length() == 0 || content.length() > 500) return "Content must have 1-500 characters";

        // We're sure that Post has correct format
        Post newPost = new Post(storageService.getNewId(), title, content, loggedUser);

        // Add post
        String x = addPostToDataStructures(loggedUser, newPost);
        if (x != null) return x;

        return "< Created new post (id=" + newPost.getId() + ")";
    }

    private String addPostToDataStructures(String loggedUser, Post newPost) {
        synchronized (storageService.storage) {
            if (storageService.storage.get(loggedUser).addPost(newPost.getId()))
                storageService.addPost(newPost);
            else return "< Some error occurred";
        }
        return null;
    }


    private String performFollow(String[] splitReq) {
        if (!isUserLogged()) return "< You must login to do this operation";
        if (!existsUser(splitReq[1])) return "< " + splitReq[1] + " does not exist";

        User loggedUserObj = storageService.storage.get(loggedUser);
        String toFollowUser = splitReq[1];
        User toFollowUserObj = storageService.storage.get(toFollowUser);

        // A user cannot follow himself
        if (Objects.requireNonNull(loggedUser).equals(toFollowUser)) return "< You cannot follow yourself!";

        synchronized (storageService.storage) {
            // If logged user already follows toFollowUser
            if (loggedUserObj.existsFollowing(toFollowUser)) return "< You already follow " + toFollowUser;

            // Add following to loggedUserObj
            loggedUserObj.addFollowing(toFollowUser);
            // Add follower to toFollowUser
            toFollowUserObj.addFollower(loggedUser);
        }

        // We need to notify the follower user that a user started following him
        try {
            asyncServer.updateNewFollower(loggedUser, loggedUserObj.tagsToString(), toFollowUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return "< now you follow " + toFollowUser;
    }

    private String performUnfollow(String[] splitReq) {
        if (!isUserLogged()) return "< You must login in order to do this operation";
        if (!existsUser(splitReq[1])) return "< " + splitReq[1] + " does not exist";

        String toUnfollowUser = splitReq[1];
        User loggedUserObj = storageService.storage.get(loggedUser);
        User toUnfollowUserObj = storageService.storage.get(toUnfollowUser);

        // A user cannot unfollow himself
        if (Objects.requireNonNull(loggedUser).equals(toUnfollowUser)) return "< You cannot unfollow yourself!";

        synchronized(storageService.storage) {
            // If logged user follows toUnfollowUser
            if (!loggedUserObj.existsFollowing(toUnfollowUser)) return "< You are not a follower of " + toUnfollowUser;

            // Add following to loggedUserObj
            loggedUserObj.removeFollowing(toUnfollowUser);
            // Add follower to toFollowUser
            toUnfollowUserObj.removeFollower(loggedUser);
        }

        // We need to notify the follower user that a user started following him
        try {
            asyncServer.updateExFollower(loggedUser, toUnfollowUser);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return "< now you're not following " + toUnfollowUser;
    }

    private String performListOperation(String[] splitReq) throws IOException {
        if (!isUserLogged()) return "< You must login in order to do this operation";

        StringBuilder stringBuilder = new StringBuilder();
        String topic = splitReq[1];
        boolean notRecognizedTopic = false;

        switch (topic) {
            case "users" -> listUsers(stringBuilder);
            case "following" -> listFollowing(stringBuilder);
            default -> notRecognizedTopic = true;
        }

        if (notRecognizedTopic) return "< " + topic + " is not listable";

        return stringBuilder.toString();
    }

    private synchronized void listFollowing(StringBuilder stringBuilder) {
        setHeaderList(stringBuilder);
        User loggedUserObj = storageService.storage.get(loggedUser);

        for (String username: loggedUserObj.getFollowings()) {
            User followingUser = storageService.storage.get(username);

            stringBuilder.append("< ").append(followingUser.toString()).append("\n");
        }
    }

    private void listUsers(StringBuilder stringBuilder) {
        setHeaderList(stringBuilder);
        ArrayList<String> loggedUserTags = storageService.storage.get(loggedUser).getTags();
        int founds = 0;

        synchronized (storageService.storage) {
            for (Map.Entry<String, User> user : storageService.storage.entrySet()) {

                if (founds == 2) break;

                // do not print the current user
                if (user.getValue().getUsername().equals(loggedUser)) continue;

                // check if user and loggedUser have tags in common
                boolean contains = false;
                for (String loggedUserTag : loggedUserTags) {
                    if (user.getValue().getTags().contains(loggedUserTag)) {
                        contains = true;
                        break;
                    }
                }

                if (contains) {
                    stringBuilder.append("< ").append(user.getValue().toString()).append("\n");
                    founds++;
                }
            }
        }

        stringBuilder.append("< \t...");
    }

    private void setHeaderList(StringBuilder stringBuilder) {
        stringBuilder.append("< \tUser\t|\tTag\n");
        stringBuilder.append("< —------------------------------------\n");
    }

    private boolean existsUser(String username) {
        return storageService.storage.containsKey(username);
    }

    private String performLogin(String[] splitReq) throws IOException {

        if (splitReq.length < 3) {
            return "< Missing Credentials";
        }

        synchronized (storageService) {
            // Check if exists a user entry inside logged user
            if (storageService.loggedUsers.containsKey(splitReq[1])) {
                if (storageService.loggedUsers.get(splitReq[1]) == client.socket()) {
                    return "< You're already logged in";
                } else {
                    return "< There is a logged in user, you must log out from it";
                }
            } else {
                if (storageService.loggedUsers.containsValue(client.socket())) {
                    return "< You're already logged in";
                }
            }

            // I am sure that the client has sent a correct format request
            if (storageService.storage.containsKey(splitReq[1])) {

                String password = storageService.storage.get(splitReq[1]).getEncryptedPassword();

                if (User.hashEncrypt(splitReq[2]).equals(password)) {
                    System.out.println("User accepted");
                    storageService.addLoggedUser(splitReq[1], client.socket());
                    return getFollowersListOutput(splitReq[1], storageService.storage)
                            + "< " + splitReq[1] + " logged in";
                } else {
                    System.out.println("Wrong Password");
                    return "< Wrong Password";
                }
            } else {
                System.out.println("User does not exists");
                return "< Error " + splitReq[1] + " does not exists";
            }
        }
    }

    private synchronized String getFollowersListOutput(String username, Map<String, User> storage) {
        StringBuilder stringBuilder = new StringBuilder();
        // You already know that user is registered
        User user = storage.get(username);

        for (String follower: user.getFollowers()) {
            stringBuilder.append(follower).append("/");
            stringBuilder.append(storage.get(follower).tagsToString()).append("//");
        }

        return stringBuilder.toString();
    }

    private String performLogout() throws IOException {
        if (isUserLogged()) {
            synchronized (storageService.loggedUsers) {
                String key = getKey(storageService.loggedUsers, client.socket());
                storageService.removeLoggedUser(key);
            }
            return "< " + key + " logged out";
        } else {
            System.out.println("No user logged in");
            return "< You're not logged in, please log in";
        }
    }

    private boolean isUserLogged() {
        return storageService.loggedUsers.containsValue(client.socket());
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
