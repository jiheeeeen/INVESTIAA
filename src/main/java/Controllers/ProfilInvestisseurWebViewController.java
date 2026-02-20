package Controllers;

import Entities.ProfilInvestisseur;
import Entities.Role;
import Entities.User;
import Services.ProfilInvestisseurCRUD;
import Utils.Session;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;

public class ProfilInvestisseurWebViewController {

    @FXML private WebView webView;

    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) engine.executeScript("window");


                window.setMember("javaBridge", bridge);
                window.setMember("javaBridgeInvest", bridge);
                window.setMember("javaBridgeInvestissement", bridge);

                engine.executeScript("window.__bridgeReady = true;");


                engine.executeScript(
                        "try{" +
                                "window.javaBridge = window.javaBridge || window.javaBridgeInvest || window.javaBridgeInvestissement;" +
                                "}catch(e){}"
                );
            }
        });

        URL page = resolvePage();
        if (page != null) engine.load(page.toExternalForm());
        else engine.loadContent("<h2>Erreur: monProfil_investisseur.html introuvable</h2>");
    }

    private URL resolvePage() {
        User u = Session.getCurrentUser();
        if (u == null) return getClass().getResource("/attenteValidation.html");
        if (u.getRole() != Role.INVESTISSEUR) return getClass().getResource("/accueil.html");
        return getClass().getResource("/monProfil_investisseur.html");
    }


    public class JavaBridge {


        public String openEditProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_edit_view.fxml", "Investia - Modifier Profil")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }



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

        // ✅ Ouvrir Projets
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

        public String goEditProfil() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Compléter Profil")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        private String json(String s) {
            if (s == null) return "null";
            String esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            return "\"" + esc + "\"";
        }
    }
}
