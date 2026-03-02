package Controllers;

import Entities.AuthUser;
import Services.UserCRUD;
import Services.auth.GoogleAuthService;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class LoginController {

    @FXML private WebView webView;

    private WebEngine engine;
    private final UserCRUD userCRUD = new UserCRUD();

    private WebAuthBridge authBridge;
    private GoogleOAuthBridge googleBridge;

    public void initialize() {
        engine = webView.getEngine();

        // Bridge classique (déjà utilisé par ton JS)
        authBridge = new WebAuthBridge(userCRUD, this::loadLogin, this::loadSignup);

        // Bridge Google (nouveau)
        googleBridge = new GoogleOAuthBridge(
                () -> new GoogleAuthService().login(),
                this::onGoogleAuthSuccess,
                this::onGoogleAuthError
        );

        // IMPORTANT: ton fichier est dans resources/html/login.html
        loadLogin();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // On garde l’existant
                window.setMember("app", authBridge);
                window.setMember("Bridge", authBridge);

                // On ajoute Google (sans casser app/Bridge)
                window.setMember("googleAuth", googleBridge);
                window.setMember("google", googleBridge);

                engine.executeScript("console.log('✅ app injected', window.app);");
                engine.executeScript("console.log('✅ googleAuth injected', window.googleAuth);");
            }
        });
    }

    private void loadLogin() {
        engine.load(getClass().getResource("/html/login.html").toExternalForm());
    }

    private void loadSignup() {
        engine.load(getClass().getResource("/html/creer_compte.html").toExternalForm());
    }

    // ===========================
    // Callbacks Google
    // ===========================

    private void onGoogleAuthSuccess(AuthUser gu) {
        System.out.println("GOOGLE OK => " + gu.getEmail() + " / " + gu.getFullName());

        // ✅ Ici tu mettras plus tard le mapping BD (sans casser le reste):
        // 1) chercher user par email
        // 2) si existe -> login
        // 3) sinon -> insert statut "en_attente"
        // 4) redirection selon statutVerification

        // Pour l’instant : message simple
        safeJsAlert("Connexion Google OK : " + gu.getEmail());
    }

    private void onGoogleAuthError(Throwable ex) {
        ex.printStackTrace();
        safeJsAlert("Erreur Google Login : " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
    }

    private void safeJsAlert(String msg) {
        if (engine == null) return;
        String safe = msg.replace("\\", "\\\\").replace("'", "\\'");
        engine.executeScript("alert('" + safe + "')");
    }

    // ===========================
    // Google bridge accessible depuis JS
    // ===========================
    public static class GoogleOAuthBridge {

        @FunctionalInterface
        public interface LoginFn {
            AuthUser run() throws Exception;
        }

        @FunctionalInterface
        public interface SuccessFn {
            void onSuccess(AuthUser user);
        }

        @FunctionalInterface
        public interface ErrorFn {
            void onError(Throwable ex);
        }

        private final LoginFn loginFn;
        private final SuccessFn successFn;
        private final ErrorFn errorFn;

        public GoogleOAuthBridge(LoginFn loginFn, SuccessFn successFn, ErrorFn errorFn) {
            this.loginFn = loginFn;
            this.successFn = successFn;
            this.errorFn = errorFn;
        }

        // Appelé depuis HTML/JS : window.googleAuth.googleLogin()
        public void googleLogin() {
            Task<AuthUser> task = new Task<>() {
                @Override
                protected AuthUser call() throws Exception {
                    return loginFn.run();
                }
            };

            task.setOnSucceeded(ev -> successFn.onSuccess(task.getValue()));
            task.setOnFailed(ev -> errorFn.onError(task.getException()));

            new Thread(task, "google-oauth-thread").start();
        }
    }
}