package Controllers;

import Entities.Investissement;
import Entities.ProfilInvestisseur;
import Entities.User;
import Services.InvestissementCRUD;
import Services.ProfilInvestisseurCRUD;
import Utils.Session;
import Utils.WebViewBridgeUtil;
import Utils.sceneManager;
import Controllers.WebAuthController;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.math.BigDecimal;
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

        // Ã¢Å“â€¦ alert(...) et confirm(...) dans WebView
        WebViewBridgeUtil.enableAlerts(engine);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                WebViewBridgeUtil.safeExec(engine,
                        "console.log('[JAVA] Bridge injected by InvestissementWebViewController');"
                );

                // Ã¢Å“â€¦ COMME AVANT : bridge page investissement seulement
                WebViewBridgeUtil.injectAll(engine, bridge, bridge);

                // Ã¢Å“â€¦ si URL contient ?idProjet=... => Session
                pushProjetIdFromUrlToSession(engine);
            }
        });

        // Ã¢Å“â€¦ Load page avec idProjet si dispo
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

        } catch (Exception ignored) { }
    }

    // =========================================================
    // Ã¢Å“â€¦ VALIDATION (TON TRAVAIL GARDÃƒâ€°)
    // =========================================================
    private static void validateMontantInvestissement(double montant, BigDecimal budgetTotal) {
        if (montant <= 0) throw new IllegalArgumentException("ERROR:MONTANT_INVALID");
        if (montant > 10000) throw new IllegalArgumentException("ERROR:MONTANT_MAX_10000");

        if (budgetTotal != null && budgetTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal m = BigDecimal.valueOf(montant);
            if (m.compareTo(budgetTotal) > 0) {
                throw new IllegalArgumentException("ERROR:MONTANT_EXCEEDS_BUDGET_TOTAL");
            }
        }
    }

    // =========================================================
    // BRIDGE JS <-> JAVA (investissement)
    // =========================================================
    public class JavaBridge {

        // =========================================================
        // Ã¢Å“â€¦ Objectif projet + total investi (TON TRAVAIL GARDÃƒâ€°)
        // =========================================================
        private BigDecimal getObjectifProjetBD(int projetId) throws Exception {
            java.sql.Connection c = Utils.MyBD.getInstance().getConn();
            String sql = "SELECT objectif_tnd FROM projet WHERE id_projet = ?";
            try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, projetId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    return rs.getBigDecimal("objectif_tnd");
                }
            }
        }

        private BigDecimal getTotalInvestiPourProjetBD(int projetId) throws Exception {
            java.sql.Connection c = Utils.MyBD.getInstance().getConn();
            String sql = "SELECT COALESCE(SUM(montant),0) AS total FROM investissement WHERE id_projet = ?";
            try (java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, projetId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getBigDecimal("total");
                }
            }
        }

        private void validateAgainstProjetObjectif(int projetId, BigDecimal montant, Integer excludeInvestmentId) throws Exception {
            BigDecimal objectif = getObjectifProjetBD(projetId);
            if (objectif == null) throw new IllegalArgumentException("ERROR:PROJET_NOT_FOUND");
            if (objectif.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("ERROR:OBJECTIF_INVALID");

            BigDecimal total = getTotalInvestiPourProjetBD(projetId);
            if (total == null) total = BigDecimal.ZERO;

            if (excludeInvestmentId != null && excludeInvestmentId > 0) {
                Investissement old = investissementCRUD.getById(excludeInvestmentId);
                if (old != null && old.getId_projet() == projetId) {
                    BigDecimal oldM = BigDecimal.valueOf(old.getMontant());
                    total = total.subtract(oldM);
                    if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;
                }
            }

            BigDecimal restant = objectif.subtract(total);
            if (restant.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("ERROR:OBJECTIF_DEJA_ATTEINT");
            if (montant.compareTo(restant) > 0) throw new IllegalArgumentException("ERROR:MONTANT_GT_OBJECTIF_RESTANT");
        }

        // ==========================
        // Header helpers (utiles ÃƒÂ  la page)
        // ==========================
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

        // ==========================
        // NAVBAR (COMME AVANT: ici dans le bridge)
        // ==========================
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

        public String openMesInvestissements() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/web/MesInvestissementsWebView.fxml", "Mes investissements")
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

        public String openWalletInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/web/wallet_investisseur_view.fxml", "Investia - Mon Wallet")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // Ã¢Å“â€¦ AJOUT MINIMAL (pour compat navbar)
        public String openWallet() {
            return openWalletInvestisseur();
        }

        // ==========================
        // Projet id (Session)
        // ==========================
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

        // ==========================
        // READ table Ã¢Å“â€¦ filtrÃƒÂ©: uniquement connectÃƒÂ©
        // ==========================
        public String listInvestissementsByProjet(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "[]";
                int pid = Integer.parseInt(idProjet.trim());

                User u = Session.getCurrentUser();
                if (u == null) return "[]";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                int currentInvestorId = (p != null ? p.getIdInvestisseur() : u.getId());

                List<Investissement> list = investissementCRUD.afficherParProjet(pid);

                StringBuilder out = new StringBuilder();
                out.append("[");
                boolean first = true;

                for (Investissement x : list) {
                    if (x.getId_investisseur() != currentInvestorId) continue;

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

        // ==========================
        // CREATE
        // ==========================
        public String createInvestissementSimple(String montant, String dateIso) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                String pidStr = getSelectedProjetId();
                if (pidStr == null || pidStr.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";
                int pid = Integer.parseInt(pidStr.trim());

                if (montant == null || montant.trim().isEmpty()) return "ERROR:MONTANT_REQUIRED";
                double m = Double.parseDouble(montant.trim());

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null && !investissementCRUD.investorColumnIsUserId()) {
                    return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";
                }
                BigDecimal budgetTotal = (p == null ? null : p.getBudgetTotal());

                try {
                    validateMontantInvestissement(m, budgetTotal);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                try {
                    validateAgainstProjetObjectif(pid, BigDecimal.valueOf(m), null);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                Date d;
                if (dateIso != null && !dateIso.trim().isEmpty()) {
                    d = Date.valueOf(dateIso.trim());
                } else {
                    d = new Date(System.currentTimeMillis());
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

        // ==========================
        // UPDATE
        // ==========================
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

                BigDecimal budgetTotal = (p == null ? null : p.getBudgetTotal());
                try {
                    validateMontantInvestissement(m, budgetTotal);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                try {
                    validateAgainstProjetObjectif(exist.getId_projet(), BigDecimal.valueOf(m), exist.getId_investissement());
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

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

        // ==========================
        // DELETE
        // ==========================
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

        // ==========================
        // NAV (retour local ÃƒÂ  projets)
        // ==========================
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

        // ==========================
        // Logout (garde ici car logique de session)
        // ==========================
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

        // ==========================
        // JSON helper
        // ==========================
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


