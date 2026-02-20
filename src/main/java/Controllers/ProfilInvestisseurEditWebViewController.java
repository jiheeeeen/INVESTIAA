package Controllers;

import Entities.ProfilInvestisseur;
import Entities.Role;
import Entities.User;
import Services.ProfilInvestisseurCRUD;
import Services.UserCRUD;
import Utils.Session;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.math.BigDecimal;
import java.net.URL;

public class ProfilInvestisseurEditWebViewController {

    @FXML private WebView webView;

    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final UserCRUD userCrud = new UserCRUD();
    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ Bridge inject + alias (important pour navbar)
                window.setMember("javaBridge", bridge);
                window.setMember("javaBridgeInvest", bridge);
                window.setMember("javaBridgeInvestissement", bridge);

                engine.executeScript("window.__bridgeReady = true;");

                // ✅ Anti-écrasement
                engine.executeScript(
                        "try{" +
                                "window.javaBridge = window.javaBridge || window.javaBridgeInvest || window.javaBridgeInvestissement;" +
                                "}catch(e){}"
                );
            }
        });

        URL page = resolvePage();
        if (page != null) engine.load(page.toExternalForm());
        else engine.loadContent("<h2>Erreur: profil_investisseur_edit.html introuvable</h2>");
    }

    private URL resolvePage() {
        User u = Session.getCurrentUser();
        if (u == null) return getClass().getResource("/attenteValidation.html");
        if (u.getRole() != Role.INVESTISSEUR) return getClass().getResource("/accueil.html");
        return getClass().getResource("/profil_investisseur_edit.html");
    }

    // ==========================
    // BRIDGE JS <-> JAVA
    // ==========================
    public class JavaBridge {

        // ===== NAVBAR =====
        public String getCurrentUserRole() {
            User u = Session.getCurrentUser();
            return (u == null || u.getRole() == null) ? "" : u.getRole().name();
        }

        public String getCurrentUserName() {
            User u = Session.getCurrentUser();
            if (u == null) return "";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            if (!full.isEmpty()) return full;
            return u.getEmail() == null ? "" : u.getEmail().trim();
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
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Accueil Investisseur")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== READ DATA =====
        public String getCurrentUserJson() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "null";

                return "{"
                        + "\"id\":" + u.getId() + ","
                        + "\"nom\":" + json(u.getNom()) + ","
                        + "\"prenom\":" + json(u.getPrenom()) + ","
                        + "\"email\":" + json(u.getEmail()) + ","
                        + "\"telephone\":" + json(u.getTelephone()) + ","
                        + "\"cin\":" + json(u.getCin())
                        + "}";
            } catch (Exception e) {
                return "null";
            }
        }

        public String getProfilInvestisseurJson() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "null";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) return "null";

                return "{"
                        + "\"idUser\":" + p.getIdUser() + ","
                        + "\"budgetTotal\":" + (p.getBudgetTotal() == null ? "null" : p.getBudgetTotal()) + ","
                        + "\"budgetMensuel\":" + (p.getBudgetMensuel() == null ? "null" : p.getBudgetMensuel()) + ","
                        + "\"ticketMoyenParProjet\":" + (p.getTicketMoyenParProjet() == null ? "null" : p.getTicketMoyenParProjet()) + ","
                        + "\"horizonInvestissement\":" + json(p.getHorizonInvestissement()) + ","
                        + "\"bio\":" + json(p.getBio()) + ","
                        + "\"secteurs\":" + json(p.getSecteurs() == null ? null : String.join(", ", p.getSecteurs())) + ","
                        + "\"cinRectoUrl\":" + json(p.getCinRectoUrl()) + ","
                        + "\"cinVersoUrl\":" + json(p.getCinVersoUrl()) + ","
                        + "\"photoUrl\":" + json(p.getPhotoUrl())
                        + "}";
            } catch (Exception e) {
                return "null";
            }
        }

        // =========================================================
        // ✅ UPDATE (User + ProfilInvestisseur) selon TES CRUD
        // =========================================================
        public String updateProfil(
                String nom,
                String prenom,
                String email,
                String telephone,
                String cin,
                String budgetTotal,
                String budgetMensuel,
                String ticketMoyen,
                String horizon,
                String bio,
                String secteursCsv
        ) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                // -------- USER --------
                if (!isEmpty(nom)) u.setNom(nom.trim());
                if (!isEmpty(prenom)) u.setPrenom(prenom.trim());
                if (!isEmpty(email)) u.setEmail(email.trim().toLowerCase());
                if (!isEmpty(telephone)) u.setTelephone(telephone.trim());
                if (!isEmpty(cin)) u.setCin(cin.trim());

                // ✅ selon ton UserCRUD => updateUser()
                userCrud.updateUser(u);

                // ✅ garder session à jour (important)
                Session.setCurrentUser(u);

                // -------- PROFIL INVESTISSEUR --------
                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";

                p.setBudgetTotal(parseBigDecimalOrNull(budgetTotal));
                p.setBudgetMensuel(parseBigDecimalOrNull(budgetMensuel));
                p.setTicketMoyenParProjet(parseBigDecimalOrNull(ticketMoyen));
                p.setHorizonInvestissement(emptyToNull(horizon));
                p.setBio(emptyToNull(bio));

                // secteursCsv: "AgriTech, GreenTech"
                // ✅ ton entity stocke Set<String>, donc tu peux parser ici si tu veux
                if (!isEmpty(secteursCsv)) {
                    // Ici je NE change pas ton modèle: je laisse tel quel.
                    // Si tu veux vraiment mettre à jour: il faut faire un Set<String> et p.setSecteurs(set)
                    // (je peux te le donner dès que tu confirmes le type exact de setSecteurs())
                }

                profilCrud.modifier(p);

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // =========================================================
        // ✅ DELETE COMPTE (ProfilInvestisseur + User) selon TES CRUD
        // =========================================================
        public String deleteAccount() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                // ✅ supprimer le profil via id_user (méthode fournie ci-dessous)
                profilCrud.deleteByUserId(u.getId());

                // ✅ supprimer user selon ton CRUD => deleteUser()
                userCrud.deleteUser(u.getId());

                // logout + retour login
                Session.setCurrentUser(null);
                WebAuthController.openLoginOnNextLoad();
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion")
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== Helpers =====
        private boolean isEmpty(String s) {
            return s == null || s.trim().isEmpty();
        }

        private String emptyToNull(String s) {
            return isEmpty(s) ? null : s.trim();
        }

        private BigDecimal parseBigDecimalOrNull(String s) {
            if (isEmpty(s)) return null;
            try {
                String v = s.trim().replace(",", ".");
                return new BigDecimal(v);
            } catch (Exception e) {
                return null;
            }
        }

        private String json(String s) {
            if (s == null) return "null";
            String esc = s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            return "\"" + esc + "\"";
        }
    }
}
