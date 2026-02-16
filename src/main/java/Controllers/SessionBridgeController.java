package Controllers;

import Controllers.WebAuthController;
import Entities.Role;
import Entities.User;
import Utils.Session;
import Utils.sceneManager;

public class SessionBridgeController {
    public String getCurrentUserName() {
        User u = Session.getCurrentUser();
        if (u == null) return "";

        String nom = u.getNom() == null ? "" : u.getNom().trim();
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        String full = (nom + " " + prenom).trim();

        if (!full.isEmpty()) return full;
        if (u.getEmail() != null) return u.getEmail().trim();
        return "";
    }

    public String getCurrentUserId() {
        User u = Session.getCurrentUser();
        if (u == null) return "";
        return String.valueOf(u.getId());
    }

    public String getCurrentUserRole() {
        User u = Session.getCurrentUser();
        if (u == null || u.getRole() == null) return "";
        Role role = u.getRole();
        return role.name();
    }

    public String logout() {
        try {
            Session.setCurrentUser(null);
            WebAuthController.openLoginOnNextLoad();
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
