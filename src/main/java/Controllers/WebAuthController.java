package Controllers;

import Entities.AuthUser;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Services.UserCRUD;
import Services.auth.GoogleAuthService;
import Utils.Session;
import Utils.MyBD;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;

public class WebAuthController {

    private static volatile boolean openLoginOnNextLoad = false;
    private static volatile boolean OPEN_LOGIN_PENDING = false;

    @FXML private WebView webView;
    private WebEngine engine;

    private final UserCRUD userCRUD = new UserCRUD();
    private WebAuthBridge authBridge;
    private FaceBridge faceBridge;
    private LoginController.GoogleOAuthBridge googleBridge;

    private ProfilBridgeController profilBridge;
    private ProjetWebContext projetContext;

    // ✅ NEW: bridge investisseur (pour completerInfos_investisseur.html)
    private ProfilInvestisseurBridgeController investBridge;

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        authBridge = new WebAuthBridge(userCRUD, this::loadLogin, this::loadSignup);
        faceBridge = new FaceBridge(userCRUD, this::goHtml);

        // ✅ entrepreneur bridge
        this.projetContext = buildProjetWebContextSafe();
        if (this.projetContext != null) {
            this.profilBridge = new ProfilBridgeController(this.projetContext);
        } else {
            System.out.println("⚠️ ProjetWebContext introuvable => profilBridge NULL (completerInfos aura Bridge indisponible)");
        }

        // ✅ investisseur bridge (ne dépend pas de ProjetWebContext)
        this.investBridge = new ProfilInvestisseurBridgeController(webView);

        googleBridge = new LoginController.GoogleOAuthBridge(
                () -> new GoogleAuthService().login(),
                this::onGoogleAuthSuccess,
                this::onGoogleAuthError
        );

        engine.setOnAlert(e -> {
            System.out.println("JS alert => " + e.getData());
            Platform.runLater(() -> {
                javafx.scene.control.Alert a =
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setTitle("JS Alert");
                a.setHeaderText(null);
                a.setContentText(e.getData());
                a.showAndWait();
            });
        });

        // ✅ injection à CHAQUE page load
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) return;

            JSObject window = (JSObject) engine.executeScript("window");

            // --- EXISTANT ---
            window.setMember("app", authBridge);
            window.setMember("Bridge", authBridge);

            window.setMember("face", faceBridge);
            window.setMember("Face", faceBridge);

            window.setMember("googleAuth", googleBridge);
            window.setMember("google", googleBridge);

            // --- ✅ NOUVEAU: choisir le bon bridge selon la page ---
            String loc = engine.getLocation();
            String lower = (loc == null) ? "" : loc.toLowerCase();

            // 1) completerInfos_investisseur.html -> inject investBridge
            if (lower.contains("completerinfos_investisseur.html")) {
                injectProfileBridge(window, investBridge, "INVESTISSEUR");
            }
            // 2) completerInfos.html (entrepreneur) -> inject profilBridge
            else if (lower.contains("completerinfos.html")) {
                if (profilBridge != null) injectProfileBridge(window, profilBridge, "ENTREPRENEUR");
                else engine.executeScript("console.warn('⚠️ profilBridge is NULL => Bridge indisponible sur completerInfos');");
            }

            engine.executeScript("console.log('✅ app injected', window.app);");
            engine.executeScript("console.log('✅ googleAuth injected', window.googleAuth);");
        });

        if (openLoginOnNextLoad) {
            openLoginOnNextLoad = false;
            loadLogin();
        } else {
            loadSignup();
        }
    }

    private void injectProfileBridge(JSObject window, Object bridge, String tag) {
        // ✅ mêmes noms partout
        window.setMember("javaBridge", bridge);
        window.setMember("javaBridgeUpload", bridge);
        window.setMember("javaBridgeInvest", bridge);
        window.setMember("bridge", bridge);
        // ⚠️ on n'écrase pas "Bridge" (réservé à authBridge)

        engine.executeScript("window.__bridgeReady = true;");
        engine.executeScript("console.log('✅ " + tag + " bridge injected', window.javaBridge);");
    }

    public static void openLoginOnNextLoad() {
        openLoginOnNextLoad = true;
    }

    public static void openLoginPendingOnNextLoad() {
        OPEN_LOGIN_PENDING = true;
        openLoginOnNextLoad();
    }

    private void loadLogin() {
        if (OPEN_LOGIN_PENDING) {
            OPEN_LOGIN_PENDING = false;
            goHtml("/html/login.html?pending=1");
        } else {
            goHtml("/html/login.html");
        }
    }

    private void loadSignup() {
        goHtml("/html/creer_compte.html");
    }

    // ===========================
    // Google Callbacks
    // ===========================
    private void onGoogleAuthSuccess(AuthUser gu) {
        try {
            String result = userCRUD.loginWithGoogle(gu.getEmail(), gu.getFullName());
            System.out.println("GOOGLE LOGIN RESULT => " + result);

            // ✅ Cas où loginWithGoogle NE met PAS la Session
            if ("PENDING".equals(result)) {
                Session.setCurrentUser(null);
                WebAuthController.openLoginPendingOnNextLoad(); // => login.html?pending=1
                loadLogin();
                return;
            }

            if ("REFUSED".equals(result)) {
                Session.setCurrentUser(null);
                safeJsAlert("Votre compte a été refusé.");
                loadLogin();
                return;
            }

            if ("INACTIVE".equals(result)) {
                Session.setCurrentUser(null);
                safeJsAlert("Compte inactif.");
                loadLogin();
                return;
            }

            // ✅ Pour OK_* / OK_NEED_PROFILE : la session doit exister
            User u = Session.getCurrentUser();
            if (u == null) {
                safeJsAlert("Erreur: session utilisateur vide après Google OAuth.");
                loadLogin();
                return;
            }

            // ✅ même logique que Face
            routeAfterAuth(u);

        } catch (Exception ex) {
            ex.printStackTrace();
            safeJsAlert("Erreur BD Google : " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            loadLogin();
        }
    }

    private void routeAfterAuth(User u) {
        // 1) ADMIN -> dashboard direct
        if (u.getRole() == Role.ADMIN) {
            goHtml("/html/admin_dashboard.html");
            return;
        }

        // 2) 1ère connexion / profil pas complété -> completerInfos selon rôle
        boolean completed = userCRUD.isProfileCompleted(u.getId());
        if (!completed) {
            if (u.getRole() == Role.INVESTISSEUR) {
                goHtml("/completerInfos_investisseur.html"); // racine resources
            } else { // ENTREPRENEUR
                goHtml("/completerInfos.html"); // racine resources
            }
            return;
        }

        // 3) Profil complété MAIS pas vérifié -> retour login + pending=1
        if (u.getStatutVerification() != StatutVerification.VERIFIE) {
            Session.setCurrentUser(null);
            openLoginPendingOnNextLoad(); // => login.html?pending=1
            loadLogin();
            return;
        }

        // 4) Vérifié MAIS inactif -> rester login (compte désactivé)
        if (!u.isActive()) {
            Session.setCurrentUser(null);
            safeJsAlert("Compte inactif.");
            loadLogin();
            return;
        }

        // 5) Vérifié + actif -> accueil selon rôle
        if (u.getRole() == Role.INVESTISSEUR) {
            goHtml("/accueil_investisseur.html");
        } else {
            goHtml("/accueil.html");
        }
    }

    private void onGoogleAuthError(Throwable ex) {
        ex.printStackTrace();
        safeJsAlert("Erreur Google Login : " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
    }

    // ===========================
    // Navigation helpers
    // ===========================
    private void redirectByRole(User u) {
        if (u == null) {
            safeJsAlert("Erreur: Session utilisateur vide.");
            return;
        }

        Role role = u.getRole();
        if (role == Role.ADMIN) {
            goHtml("/html/admin_dashboard.html");
            return;
        }

        if (role == Role.INVESTISSEUR) {
            goHtml("/accueil_investisseur.html");
        } else {
            goHtml("/accueil.html");
        }
    }

    private void goHtml(String pathWithQuery) {
        String[] parts = pathWithQuery.split("\\?", 2);
        String path = parts[0];
        String query = (parts.length > 1) ? "?" + parts[1] : "";

        var url = getClass().getResource(path);
        if (url == null) {
            System.out.println("❌ Page introuvable (resources) : " + pathWithQuery);
            safeJsAlert("Page introuvable : " + pathWithQuery);
            return;
        }

        Platform.runLater(() -> {
            String finalUrl = url.toExternalForm() + query;
            System.out.println("➡️ Redirecting to: " + finalUrl);
            engine.load(finalUrl);
        });
    }

    private void safeJsAlert(String msg) {
        if (engine == null) return;
        String safe = msg.replace("\\", "\\\\").replace("'", "\\'");
        engine.executeScript("alert('" + safe + "')");
    }

    // ======================================================
    // ProjetWebContext builder (safe)
    // ======================================================
    private ProjetWebContext buildProjetWebContextSafe() {
        try {
            try {
                Method m = ProjetWebContext.class.getMethod("getInstance");
                Object obj = m.invoke(null);
                if (obj instanceof ProjetWebContext ctx) {
                    try {
                        Method setWv = ProjetWebContext.class.getMethod("setWebView", WebView.class);
                        setWv.invoke(ctx, webView);
                    } catch (Exception ignored) {}
                    return ctx;
                }
            } catch (Exception ignored) {}

            try {
                Constructor<ProjetWebContext> c = ProjetWebContext.class.getConstructor(WebView.class);
                return c.newInstance(webView);
            } catch (Exception ignored) {}

            try {
                Constructor<ProjetWebContext> c = ProjetWebContext.class.getConstructor(WebView.class, Connection.class);
                return c.newInstance(webView, MyBD.getInstance().getConn());
            } catch (Exception ignored) {}

            try {
                Constructor<ProjetWebContext> c = ProjetWebContext.class.getConstructor();
                ProjetWebContext ctx = c.newInstance();
                try {
                    Method setWv = ProjetWebContext.class.getMethod("setWebView", WebView.class);
                    setWv.invoke(ctx, webView);
                } catch (Exception ignored) {}
                return ctx;
            } catch (Exception ignored) {}

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

