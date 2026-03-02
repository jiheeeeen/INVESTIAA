package Controllers;

import Entities.ProfilInvestisseur;
import Entities.Role;
import Entities.User;
import Services.ProfilInvestisseurCRUD;
import Services.UserCRUD;
import Utils.Session;
import Utils.WebViewBridgeUtil;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

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

                // âœ… Debug (optionnel)
                WebViewBridgeUtil.safeExec(engine,
                        "console.log('[JAVA] Bridge injected by ProfilInvestisseurEditWebViewController');"
                );

                // âœ… CLEAN: javaBridge + alias + __bridgeReady + anti-Ã©crasement + callback
                WebViewBridgeUtil.injectAll(engine, bridge, bridge);
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
        public String openMesInvestissements() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorMesInvestissements);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openContactInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorContact);
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

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProfil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProjets);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorAccueil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // âœ… AJOUT MINIMAL : WALLET
        public String openWalletInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorWallet);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // âœ… AJOUT MINIMAL : alias (si navbar appelle openWallet)
        public String openWallet() {
            return openWalletInvestisseur();
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
        // âœ… UPDATE (User + ProfilInvestisseur) + VALIDATION BUDGET/TICKET
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

                // -------- VALIDATION STRICTE (AJOUT) --------
                BigDecimal bt;
                BigDecimal bm;
                BigDecimal tk;
                try {
                    bt = requirePositiveBD(budgetTotal, "ERROR:BUDGET_TOTAL_REQUIRED");
                    bm = optionalPositiveBD(budgetMensuel); // null autorisÃ© si vide
                    tk = requirePositiveBD(ticketMoyen, "ERROR:TICKET_REQUIRED");

                    validateBudgetsAndTicket(bt, bm, tk);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                // -------- USER --------
                if (!isEmpty(nom)) u.setNom(nom.trim());
                if (!isEmpty(prenom)) u.setPrenom(prenom.trim());
                if (!isEmpty(email)) u.setEmail(email.trim().toLowerCase());
                if (!isEmpty(telephone)) u.setTelephone(telephone.trim());
                if (!isEmpty(cin)) u.setCin(cin.trim());

                userCrud.updateUser(u);
                Session.setCurrentUser(u);

                // -------- PROFIL INVESTISSEUR --------
                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";

                // âœ… appliquer les valeurs VALIDÃ‰ES
                p.setBudgetTotal(bt);
                p.setBudgetMensuel(bm);
                p.setTicketMoyenParProjet(tk);

                p.setHorizonInvestissement(emptyToNull(horizon));
                p.setBio(emptyToNull(bio));

                // (tu peux parser secteursCsv plus tard si tu veux)
                profilCrud.modifier(p);

                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil")
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // =========================================================
        // âœ… DELETE COMPTE (ProfilInvestisseur + User) selon TES CRUD
        // =========================================================
        public String deleteAccount() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                profilCrud.deleteByUserId(u.getId());
                userCrud.deleteUser(u.getId());

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

        // (je garde ton ancien helper au cas oÃ¹ tu lâ€™utilises ailleurs)
        private BigDecimal parseBigDecimalOrNull(String s) {
            if (isEmpty(s)) return null;
            try {
                String v = s.trim().replace(",", ".");
                return new BigDecimal(v);
            } catch (Exception e) {
                return null;
            }
        }

        // âœ… AJOUT: parsing strict
        private BigDecimal requirePositiveBD(String s, String errorCode) {
            if (isEmpty(s)) throw new IllegalArgumentException(errorCode);
            BigDecimal bd = new BigDecimal(s.trim().replace(",", "."));
            if (bd.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(errorCode);
            return bd;
        }

        // âœ… AJOUT: parsing optionnel mais strict si rempli
        private BigDecimal optionalPositiveBD(String s) {
            if (isEmpty(s)) return null;
            BigDecimal bd = new BigDecimal(s.trim().replace(",", "."));
            if (bd.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("ERROR:BUDGET_MENSUEL_INVALID");
            return bd;
        }

        // âœ… AJOUT: validation mÃ©tier
        private void validateBudgetsAndTicket(BigDecimal budgetTotal, BigDecimal budgetMensuel, BigDecimal ticket) {
            // budgetTotal > budgetMensuel (si mensuel existe)
            if (budgetMensuel != null && budgetTotal.compareTo(budgetMensuel) <= 0) {
                throw new IllegalArgumentException("ERROR:BUDGET_TOTAL_MUST_BE_GREATER_THAN_MONTHLY");
            }

            // ticket <= 10000
            if (ticket.compareTo(new BigDecimal("10000")) > 0) {
                throw new IllegalArgumentException("ERROR:TICKET_MAX_10000");
            }

            // ticket <= budgetTotal
            if (ticket.compareTo(budgetTotal) > 0) {
                throw new IllegalArgumentException("ERROR:TICKET_MUST_BE_LESS_OR_EQUAL_BUDGET_TOTAL");
            }

            // (optionnel logique) ticket <= budgetMensuel si mensuel existe
            if (budgetMensuel != null && ticket.compareTo(budgetMensuel) > 0) {
                throw new IllegalArgumentException("ERROR:TICKET_MUST_BE_LESS_OR_EQUAL_BUDGET_MENSUEL");
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

