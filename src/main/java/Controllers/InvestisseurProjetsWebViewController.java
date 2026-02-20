package Controllers;

import Entities.Projet;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Services.ProjetCRUD;
import Utils.Session;
import Utils.sceneManager;
import Controllers.WebAuthController;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvestisseurProjetsWebViewController {

    @FXML
    private WebView webView;

    private final ProjetCRUD crud = new ProjetCRUD();
    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        System.out.println("✅ OPEN: InvestisseurProjetsWebViewController");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ Bridge inject + alias (cohérent avec navbar.js + autres pages)
                window.setMember("javaBridge", bridge);
                window.setMember("javaBridgeInvest", bridge);
                window.setMember("javaBridgeInvestissement", bridge);

                engine.executeScript("window.__bridgeReady = true;");

                // ✅ anti-écrasement : si un script remplace javaBridge, on le restaure
                engine.executeScript(
                        "try{" +
                                "window.javaBridge = window.javaBridge || window.javaBridgeInvest || window.javaBridgeInvestissement;" +
                                "}catch(e){}"
                );
            }
        });

        URL page = resolvePage();
        if (page != null) engine.load(page.toExternalForm());
        else engine.loadContent("<h2>Erreur: projet_view_investisseur.html introuvable</h2>");
    }

    private URL resolvePage() {
        User current = Session.getCurrentUser();
        if (current == null) return getClass().getResource("/attenteValidation.html");

        if (current.getRole() != Role.INVESTISSEUR) return getClass().getResource("/accueil.html");

        if (current.getStatutVerification() != StatutVerification.VERIFIE || !current.isActive()) {
            return getClass().getResource("/attenteValidation.html");
        }

        return getClass().getResource("/projet_view_investisseur.html");
    }

    // ==========================
    // BRIDGE
    // ==========================
    public class JavaBridge {

        // ✅ navbar : ouvrir EDIT profil investisseur (UNE SEULE FOIS)
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

        // ✅ navbar : refresh (UNE SEULE FOIS)
        public String refreshProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String setSelectedProjetId(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";
                int pid = Integer.parseInt(idProjet.trim());
                if (pid <= 0) return "ERROR:ID_PROJET_INVALID";
                Session.setSelectedProjetId(pid);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
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

        public String getCurrentUserRole() {
            User u = Session.getCurrentUser();
            return (u == null || u.getRole() == null) ? "" : u.getRole().name();
        }

        // ✅ appelé par projet_view_investisseur.html
        public String listProjets() {
            try {
                List<Projet> list = crud.afficher();
                StringBuilder sb = new StringBuilder();
                sb.append("[");

                for (int i = 0; i < list.size(); i++) {
                    Projet p = list.get(i);
                    if (!"VALIDATED".equals(mapStatusForUi(p.getStatut()))) {
                        continue;
                    }
                    if (sb.length() > 1) sb.append(",");
                    sb.append(toListJson(p));
                }

                sb.append("]");
                return sb.toString();
            } catch (SQLException e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ✅ détails projet pour detailsInvestisseur.html
        public String getProjetById(String id) {
            try {
                int projectId = Integer.parseInt(id);
                Projet p = crud.getById(projectId);
                if (p == null) return "null";
                return toDetailJson(p);
            } catch (Exception e) {
                return "null";
            }
        }

        // ✅ IMPORTANT : ouvrir l'écran Investir via JavaFX (met Session.selectedProjetId)
        public String openInvestir(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";

                int pid = Integer.parseInt(idProjet.trim());
                if (pid <= 0) return "ERROR:ID_PROJET_INVALID";

                // ✅ stocker l'id projet pour l'écran investissement
                Session.setSelectedProjetId(pid);

                // ✅ Ouvrir le bon écran: celui qui contient le WebView investissement
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investissement_view.fxml", "Investia - Investir")
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ✅ navbar : Accueil
        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Accueil Investisseur")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // ✅ navbar : Projets
        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // ✅ navbar : ouvrir profil investisseur
        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil (Investisseur)")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
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

    // ==========================
    // JSON helpers
    // ==========================
    private static String toListJson(Projet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":").append(p.getIdProjet()).append(",");
        sb.append("\"title\":").append(jsonString(p.getTitre())).append(",");
        sb.append("\"category\":").append(jsonString(p.getSecteur())).append(",");
        sb.append("\"short\":").append(jsonString(p.getDescriptionCourte())).append(",");
        sb.append("\"goal\":").append(p.getObjectifTnd() == null ? "0" : p.getObjectifTnd()).append(",");
        sb.append("\"status\":").append(jsonString(mapStatusForUi(p.getStatut()))).append(",");
        sb.append("\"updatedAt\":").append(jsonString(formatDate(p.getUpdatedAt())));
        sb.append("}");
        return sb.toString();
    }

    private static String toDetailJson(Projet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"idProjet\":").append(p.getIdProjet()).append(",");
        sb.append("\"entrepreneurId\":").append(p.getEntrepreneurId()).append(",");
        sb.append("\"statut\":").append(jsonString(p.getStatut())).append(",");
        sb.append("\"titre\":").append(jsonString(p.getTitre())).append(",");
        sb.append("\"secteur\":").append(jsonString(p.getSecteur())).append(",");
        sb.append("\"descriptionCourte\":").append(jsonString(p.getDescriptionCourte())).append(",");
        sb.append("\"descriptionLongue\":").append(jsonString(p.getDescriptionLongue())).append(",");
        sb.append("\"objectifTnd\":").append(p.getObjectifTnd() == null ? "0" : p.getObjectifTnd()).append(",");
        sb.append("\"dureeCampagneJours\":").append(p.getDureeCampagneJours()).append(",");
        sb.append("\"modeRemboursement\":").append(jsonString(p.getModeRemboursement())).append(",");
        sb.append("\"tauxInteretPct\":").append(p.getTauxInteretPct() == null ? "null" : p.getTauxInteretPct()).append(",");
        sb.append("\"dureeRemboursementMois\":").append(p.getDureeRemboursementMois() == null ? "null" : p.getDureeRemboursementMois()).append(",");
        sb.append("\"createdAt\":").append(jsonString(formatTimestamp(p.getCreatedAt()))).append(",");
        sb.append("\"updatedAt\":").append(jsonString(formatTimestamp(p.getUpdatedAt())));
        sb.append("}");
        return sb.toString();
    }

    private static String mapStatusForUi(String statut) {
        if (statut == null) return "DRAFT";
        switch (statut) {
            case "BROUILLON": return "DRAFT";
            case "EN_ATTENTE": return "PENDING";
            case "VALIDE": return "VALIDATED";
            case "REFUSE": return "REJECTED";
            default: return "DRAFT";
        }
    }

    private static String formatDate(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().toLocalDate().format(DateTimeFormatter.ISO_DATE);
    }

    private static String formatTimestamp(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
