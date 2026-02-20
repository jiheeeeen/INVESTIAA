package Controllers;

import Entities.Investissement;
import Entities.ProfilInvestisseur;
import Entities.User;
import Services.InvestissementCRUD;
import Services.ProfilInvestisseurCRUD;
import Utils.Session;
import Utils.sceneManager;
import Controllers.WebAuthController;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.sql.Date;
import java.util.List;

public class InvestissementWebViewController {

    @FXML private WebView webView;

    private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        final WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // ✅ AJOUT (sans changer ta logique) :
        // Active confirm("...") et alert("...") dans JavaFX WebView
        engine.setConfirmHandler(message -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText(message);

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK;
        });

        engine.setOnAlert(event -> {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Message");
            alert.setHeaderText(null);
            alert.setContentText(event.getData());
            alert.showAndWait();
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) engine.executeScript("window");
                engine.executeScript("console.log('[JAVA] Bridge injected by InvestissementWebViewController');");

                // ✅ Inject bridge + alias
                window.setMember("javaBridge", bridge);
                window.setMember("javaBridgeInvest", bridge);
                window.setMember("javaBridgeInvestissement", bridge);

                engine.executeScript("window.__bridgeReady = true;");

                // ✅ Anti-écrasement (navbar.js)
                engine.executeScript(
                        "try {" +
                                "window.javaBridge = window.javaBridge || window.javaBridgeInvest || window.javaBridgeInvestissement;" +
                                "} catch(e) {}"
                );

                // ✅ Si l’URL contient ?idProjet=... => Session
                pushProjetIdFromUrlToSession(engine);
            }
        });

        // ✅ Load page avec idProjet si dispo
        URL page = getClass().getResource("/investissement_form.html");
        if (page != null) {
            Integer pid = Session.getSelectedProjetId();
            String url = page.toExternalForm();
            if (pid != null && pid > 0) url = url + "?idProjet=" + pid;
            engine.load(url);
        } else {
            engine.loadContent("<h2>investissement_form.html introuvable</h2>");
        }
    }

    private void pushProjetIdFromUrlToSession(WebEngine engine) {
        try {
            Object pidFromUrl = engine.executeScript(
                    "(function(){ try{ return new URLSearchParams(location.search).get('idProjet') || ''; }catch(e){ return ''; } })();"
            );
            if (pidFromUrl == null) return;

            String s = pidFromUrl.toString().trim();
            if (s.isEmpty()) return;

            int pid = Integer.parseInt(s);
            if (pid > 0) Session.setSelectedProjetId(pid);

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // BRIDGE JS <-> JAVA
    // =========================================================
    public class JavaBridge {

        // ===== navbar helpers =====
        public String getCurrentUserRole() {
            try {
                User u = Session.getCurrentUser();
                return (u == null || u.getRole() == null) ? "" : u.getRole().name();
            } catch (Exception e) {
                return "";
            }
        }

        public String getCurrentUserName() {
            User u = Session.getCurrentUser();
            if (u == null) return "-";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            return full.isEmpty() ? (u.getEmail() == null ? "-" : u.getEmail().trim()) : full;
        }

        // ===== Projet id =====
        public String getSelectedProjetId() {
            try {
                Integer pid = Session.getSelectedProjetId();
                return (pid == null || pid <= 0) ? "" : String.valueOf(pid);
            } catch (Exception e) {
                return "";
            }
        }

        public String setSelectedProjetId(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";
                int pid = Integer.parseInt(idProjet.trim());
                if (pid <= 0) return "ERROR:ID_PROJET_INVALID";
                Session.setSelectedProjetId(pid);
                return "OK";
            } catch (NumberFormatException e) {
                return "ERROR:ID_PROJET_INVALID";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== READ table =====
        public String listInvestissementsByProjet(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "[]";
                int pid = Integer.parseInt(idProjet.trim());

                List<Investissement> list = investissementCRUD.afficherParProjet(pid);

                StringBuilder out = new StringBuilder();
                out.append("[");
                boolean first = true;
                for (Investissement x : list) {
                    if (!first) out.append(",");
                    first = false;

                    out.append("{");
                    out.append("\"id_investissement\":").append(x.getId_investissement()).append(",");
                    out.append("\"montant\":").append(x.getMontant()).append(",");
                    out.append("\"date_investissement\":").append(json(x.getDate_investissement() == null ? "" : x.getDate_investissement().toString())).append(",");
                    out.append("\"id_investisseur\":").append(x.getId_investisseur()).append(",");
                    out.append("\"id_projet\":").append(x.getId_projet());
                    out.append("}");
                }
                out.append("]");
                return out.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ===== CREATE =====
        public String createInvestissementSimple(String montant, String dateIso) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                String pidStr = getSelectedProjetId();
                if (pidStr == null || pidStr.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";
                int pid = Integer.parseInt(pidStr.trim());

                if (montant == null || montant.trim().isEmpty()) return "ERROR:MONTANT_REQUIRED";
                double m = Double.parseDouble(montant.trim());
                if (m <= 0) return "ERROR:MONTANT_INVALID";

                Date d;
                if (dateIso != null && !dateIso.trim().isEmpty()) {
                    d = Date.valueOf(dateIso.trim()); // yyyy-mm-dd
                } else {
                    d = new Date(System.currentTimeMillis());
                }

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null && !investissementCRUD.investorColumnIsUserId()) {
                    return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";
                }

                Investissement inv = new Investissement();
                inv.setMontant(m);
                inv.setDate_investissement(d);
                inv.setId_investisseur(p != null ? p.getIdInvestisseur() : u.getId());
                inv.setId_projet(pid);

                investissementCRUD.ajouter(inv);
                return "OK:" + inv.getId_investissement();

            } catch (NumberFormatException e) {
                return "ERROR:INVALID_NUMBER";
            } catch (IllegalArgumentException e) {
                return "ERROR:DATE_INVALID (yyyy-mm-dd)";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== UPDATE =====
        public String updateInvestissement(String idInv, String montant, String dateIso) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                if (idInv == null || idInv.trim().isEmpty()) return "ERROR:ID_REQUIRED";
                int id = Integer.parseInt(idInv.trim());

                Investissement exist = investissementCRUD.getById(id);
                if (exist == null) return "ERROR:NOT_FOUND";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null && !investissementCRUD.investorColumnIsUserId()) {
                    return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";
                }

                int currentInvestorId = (p != null ? p.getIdInvestisseur() : u.getId());
                if (exist.getId_investisseur() != currentInvestorId) return "ERROR:FORBIDDEN";

                if (montant == null || montant.trim().isEmpty()) return "ERROR:MONTANT_REQUIRED";
                double m = Double.parseDouble(montant.trim());
                if (m <= 0) return "ERROR:MONTANT_INVALID";

                Date d = (dateIso != null && !dateIso.trim().isEmpty())
                        ? Date.valueOf(dateIso.trim())
                        : (exist.getDate_investissement() == null
                        ? new Date(System.currentTimeMillis())
                        : exist.getDate_investissement());

                exist.setMontant(m);
                exist.setDate_investissement(d);

                investissementCRUD.modifier(exist);
                return "OK";

            } catch (NumberFormatException e) {
                return "ERROR:INVALID_NUMBER";
            } catch (IllegalArgumentException e) {
                return "ERROR:DATE_INVALID (yyyy-mm-dd)";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== DELETE =====
        // ✅ CHANGEMENT MINIMAL : accepte Object pour gérer "10", "10.0", Number/Double venant de JS
        public String deleteInvestissement(Object idInv) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                if (idInv == null) return "ERROR:ID_REQUIRED";

                int id;
                if (idInv instanceof Number) {
                    id = ((Number) idInv).intValue();
                } else {
                    String s = idInv.toString().trim();
                    if (s.isEmpty()) return "ERROR:ID_REQUIRED";
                    if (s.contains(".")) s = s.substring(0, s.indexOf('.'));
                    id = Integer.parseInt(s);
                }

                Investissement exist = investissementCRUD.getById(id);
                if (exist == null) return "ERROR:NOT_FOUND";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";

                if (exist.getId_investisseur() != p.getIdInvestisseur()) return "ERROR:FORBIDDEN";

                investissementCRUD.supprimer(id);
                return "OK";

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== NAV =====
        public String backToProjets() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
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
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ===== JSON helper =====
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
