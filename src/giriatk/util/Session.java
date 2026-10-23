package giriatk.util;

import giriatk.model.User;

public final class Session {

    private static User current;

    private Session() {
    }

    public static void login(User user) {
        current = user;
    }

    public static void logout() {
        current = null;
    }

    public static User user() {
        return current;
    }

    public static boolean isLoggedIn() {
        return current != null;
    }

    public static boolean isOwner() {
        return current != null && current.isOwner();
    }
}
