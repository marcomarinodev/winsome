public class SystemCodes {
    public static String SUCCESS = "Success";
    public static String MISSING_PASSWORD = "Bad Password";
    public static String WEAK_PASSWORD = "Weak Password";
    public static String MISSING_USERNAME = "Bad Username";
    public static String USER_ALREADY_EXISTS = "User Already Exists";
    public static String MISSING_TAGS = "Bad Tags";

    public static void printOperationResult(String out, String operation) {
        System.out.println(out + " on " + operation);
    }

}
