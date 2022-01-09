import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PersistentOperator {

    public static synchronized Pair<Map<String, User>, Map<String, Post>> persistentRead(
            String usersFilename,
            String postsFilename) {
        Map<String, User> storage = new ConcurrentHashMap<>();
        Map<String, Post> posts = new ConcurrentHashMap<>();
        String basePath = new File("").getAbsolutePath();
        String usersFullPath = basePath + "/" + usersFilename;
        String postsFullPath = basePath + "/" + postsFilename;

        try {
            FileInputStream fileInputStream = new FileInputStream(usersFullPath);
            JsonReader jsonReader = new JsonReader(new InputStreamReader(fileInputStream));

            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                User user = new Gson().fromJson(jsonReader, User.class);
                storage.put(user.getUsername(), user);
            }
            jsonReader.endArray();

            fileInputStream = new FileInputStream(postsFullPath);
            jsonReader = new JsonReader(new InputStreamReader(fileInputStream));

            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                Post post = new Gson().fromJson(jsonReader, Post.class);
                posts.put(post.getId(), post);
            }
            jsonReader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return new Pair(storage, posts);
        }
    }

    public static synchronized void persistentWrite(Map<String, User> storage,
                                       Map<String, Post> posts,
                                       String usersFilename,
                                       String postsFilename) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String basePath = new File("").getAbsolutePath();
        int lastStorageIndex = storage.size() - 1;
        int lastPostsIndex = posts.size() - 1;
        int currentIndex = 0;
        StringBuilder stringBuilder = new StringBuilder();

        // users
        stringBuilder.append("[\n");
        for (Map.Entry<String, User> entry: storage.entrySet()) {
            String entryJson = gson.toJson(entry.getValue());
            stringBuilder.append(entryJson);
            if (currentIndex < lastStorageIndex)
                stringBuilder.append(",\n");
            currentIndex++;
        }
        currentIndex = 0;
        stringBuilder.append("\n]\n");
        writeOnFile(usersFilename, basePath, stringBuilder);

        // posts
        stringBuilder = new StringBuilder();
        stringBuilder.append("[\n");
        for (Map.Entry<String, Post> entry: posts.entrySet()) {
            String entryJson = gson.toJson(entry.getValue());
            stringBuilder.append(entryJson);
            if (currentIndex < lastPostsIndex)
                stringBuilder.append(",\n");
            currentIndex++;
        }
        stringBuilder.append("\n]\n");
        writeOnFile(postsFilename, basePath, stringBuilder);

    }

    private static void writeOnFile(String usersFilename, String basePath, StringBuilder stringBuilder) {
        String fullPath = basePath + "/" + usersFilename;
        try {
            FileWriter myWriter = new FileWriter(fullPath);
            myWriter.write(stringBuilder.toString());
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
