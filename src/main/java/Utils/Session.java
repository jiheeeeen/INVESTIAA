package Utils;

import Entities.User;

public class Session {
    private static User currentUser;
    private static int selectedProjetId;

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    public static int getSelectedProjetId() { return selectedProjetId; }
    public static void setSelectedProjetId(int id) { selectedProjetId = id; }
}
