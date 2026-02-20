package Controllers;

import Entities.Role;
import Services.UserCRUD;
import Utils.Session;
import Utils.sceneManager;

public class WebAuthBridge {

    private final UserCRUD userCRUD;
    private final Runnable goLogin;
    private final Runnable goSignup;

    public WebAuthBridge(UserCRUD userCRUD, Runnable goLogin, Runnable goSignup) {
        this.userCRUD = userCRUD;
        this.goLogin = goLogin;
        this.goSignup = goSignup;
    }

    // ===== NAV =====
    public void goLogin() { goLogin.run(); }
    public void goSignup() { goSignup.run(); }

    // ===== REGISTER (6 params) =====
    // Retourne: OK / EMAIL_EXISTS / ROLE_INVALID / ERROR_xxx
    public String register(String fullName, String email, String password, String role,
                           String telephone, String cin) {
        try {
            return userCRUD.registerPending(fullName, email, password, role, telephone, cin);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR_" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    // ===== LOGIN =====
    public String login(String email, String password) {
        try {
            String res = userCRUD.login(email, password);

            if ("OK_ADMIN".equals(res)) {
                // navigation admin
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/dashboard_admin.fxml", "Investia - Dashboard Admin")
                );
            } else if ("OK_NEED_PROFILE".equals(res)) {
                // premiere connexion: completer les informations
                javafx.application.Platform.runLater(() -> {
                    if (Session.getCurrentUser() != null && Session.getCurrentUser().getRole() == Role.INVESTISSEUR) {
                        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Completer vos informations");
                    } else {
                        sceneManager.switchTo("/projet_view.fxml", "Investia - Completer vos informations");
                    }
                });
            } else if ("OK_USER".equals(res)) {
                // navigation utilisateur vers l'accueil
                javafx.application.Platform.runLater(() -> {
                    if (Session.getCurrentUser() != null && Session.getCurrentUser().getRole() == Role.INVESTISSEUR) {
                        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Accueil Investisseur");
                    } else {
                        sceneManager.switchTo("/projet_view.fxml", "Investia - Accueil");
                    }
                });
            }
            return res;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR_" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }
}
