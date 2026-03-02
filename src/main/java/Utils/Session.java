package Utils;

import Entities.User;

public class Session {
    private static volatile User currentUser;
    private static volatile int selectedProjetId;
    private static volatile int selectedContactUserId;

    public static User getCurrentUser() { return currentUser; }
    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
        if (user == null) {
            selectedProjetId = 0;
            selectedContactUserId = 0;
        }
    }

    public static int getSelectedProjetId() { return selectedProjetId; }
    public static void setSelectedProjetId(int id) { selectedProjetId = id; }

    public static int getSelectedContactUserId() { return selectedContactUserId; }
    public static void setSelectedContactUserId(int id) { selectedContactUserId = id; }
}
