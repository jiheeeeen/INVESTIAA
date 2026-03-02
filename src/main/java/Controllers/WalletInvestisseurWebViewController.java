package Controllers;

import Utils.WebViewBridgeUtil;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WalletInvestisseurWebViewController {

    @FXML
    private WebView webView;

    private final JavaBridge bridge = new JavaBridge();

    @FXML
    private void initialize() {
        System.out.println("âœ… OPEN: WalletInvestisseurWebViewController");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                WebViewBridgeUtil.safeExec(engine,
                        "console.log('[JAVA] Bridge injected by WalletInvestisseurWebViewController');");

                WebViewBridgeUtil.injectNavBridge(engine, bridge);

                WebViewBridgeUtil.safeExec(engine,
                        "window.javaBridge = window.navBridge || window.javaBridge; window.__bridgeReady = true; if(window.__onJavaBridgeReady) window.__onJavaBridgeReady();");
            }
        });

        URL html = getClass().getResource("/web/wallet_investisseur.html");
        if (html != null) engine.load(html.toExternalForm());
        else engine.loadContent("<h2>wallet_investisseur.html introuvable (/web)</h2>");
    }

    public class JavaBridge {

        private final Services.ExchangeRateService exchangeRateService = new Services.ExchangeRateService();
        private final MessagerieBridgeController messagerieController = new MessagerieBridgeController();

        public String getCurrentUserName() {
            Entities.User u = Utils.Session.getCurrentUser();
            if (u == null) return "";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            if (!full.isEmpty()) return full;
            return u.getEmail() == null ? "" : u.getEmail().trim();
        }

        public String getCurrentUserRole() {
            Entities.User u = Utils.Session.getCurrentUser();
            return (u == null || u.getRole() == null) ? "" : u.getRole().name();
        }

        public String getCurrentUserId() {
            Entities.User u = Utils.Session.getCurrentUser();
            return u == null ? "" : String.valueOf(u.getId());
        }

        public String setSelectedContactUserId(String id) { return messagerieController.setSelectedContactUserId(id); }
        public String getSelectedContactUserId() { return messagerieController.getSelectedContactUserId(); }
        public String getMessagerieContactsJson() { return messagerieController.getMessagerieContactsJson(); }
        public String getConversationMessagesJson(String otherUserId) { return messagerieController.getConversationMessagesJson(otherUserId); }
        public String sendMessageToUser(String otherUserId, String contenu) { return messagerieController.sendMessageToUser(otherUserId, contenu); }
        public String getUserSummaryJson(String userId) { return messagerieController.getUserSummaryJson(userId); }
        public String getUnreadMessagesCount() { return messagerieController.getUnreadMessagesCount(); }

        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorAccueil);
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

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProfil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openEditProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorEditProfil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openWalletInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorWallet);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openWallet() { return openWalletInvestisseur(); }

        public String getTndToEurUsdRatesJson() {
            try {
                return exchangeRateService.getTndToEurUsdRatesJson();
            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\"}";
            }
        }

        // =========================================================
        // âœ… WALLET STATE (TA LOGIQUE + AJOUT CREDIT REMBOURSEMENT PAYE)
        // =========================================================
        public String getWalletStateJson() {
            try {
                Entities.User u = Utils.Session.getCurrentUser();
                if (u == null) return "{\"error\":true,\"message\":\"USER_NOT_CONNECTED\"}";

                Services.ProfilInvestisseurCRUD profilCrud = new Services.ProfilInvestisseurCRUD();
                Entities.ProfilInvestisseur profil = profilCrud.getByUserId(u.getId());

                if (profil == null || profil.getBudgetTotal() == null) {
                    return "{\"error\":true,\"message\":\"PROFIL_OR_BUDGET_TOTAL_MISSING\"}";
                }

                // âœ… id_investisseur (clÃ©)
                int idInvestisseur = profil.getIdInvestisseur();

                // =========================================================
                // âœ… 1) CREDIT: rÃ©cupÃ¨re remboursements PAYE (montant_paye>0),
                //    ajoute au budget_total, sauvegarde, puis met montant_paye=0
                // =========================================================
                Services.RemboursementCRUD remboursementCRUD = new Services.RemboursementCRUD();
                List<Services.RemboursementCRUD.PaidCreditRow> creditsRows =
                        remboursementCRUD.getPaidCreditablesByInvestisseur(idInvestisseur);

                BigDecimal creditedTotal = BigDecimal.ZERO;
                List<Integer> idsToConsume = new ArrayList<>();

                if (creditsRows != null && !creditsRows.isEmpty()) {
                    for (Services.RemboursementCRUD.PaidCreditRow row : creditsRows) {
                        double mp = row.getMontantPaye();
                        if (!Double.isNaN(mp) && mp > 0) {
                            creditedTotal = creditedTotal.add(BigDecimal.valueOf(mp));
                            idsToConsume.add(row.getRemboursementId());
                        }
                    }

                    if (creditedTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal budgetCourantDB = profil.getBudgetTotal();
                        BigDecimal newBudgetCourant = budgetCourantDB.add(creditedTotal);

                        // âœ… update DB budget_total
                        profilCrud.updateBudgetTotalByIdInvestisseur(idInvestisseur, newBudgetCourant);

                        // âœ… consommer pour ne pas re-crÃ©diter (sans changer structure DB)
                        remboursementCRUD.consumeMontantPayeByIds(idsToConsume);

                        // âœ… update objet
                        profil.setBudgetTotal(newBudgetCourant);
                    }
                }

                // =========================================================
                // âœ… 2) TA LOGIQUE EXISTANTE (INCHANGÃ‰E)
                // =========================================================
                BigDecimal budgetCourant = profil.getBudgetTotal();

                Services.InvestissementCRUD investissementCRUD = new Services.InvestissementCRUD();

                int keyForSum = idInvestisseur;
                double sum = investissementCRUD.sumMontantParInvestisseur(keyForSum);
                BigDecimal totalInvesti = BigDecimal.valueOf(sum);

                BigDecimal budgetInitial = budgetCourant.add(totalInvesti);

                BigDecimal budgetRestant = budgetCourant;
                if (budgetRestant.compareTo(BigDecimal.ZERO) < 0) budgetRestant = BigDecimal.ZERO;

                // =========================================================
                // âœ… 3) Historique (sans DB) -> renvoyÃ© en JSON
                // credits: [{id,montant,date}]
                // =========================================================
                StringBuilder creditsJson = new StringBuilder();
                creditsJson.append("[");

                boolean first = true;
                if (creditsRows != null && !creditsRows.isEmpty()) {
                    for (Services.RemboursementCRUD.PaidCreditRow row : creditsRows) {
                        double mp = row.getMontantPaye();
                        if (!Double.isNaN(mp) && mp > 0) {
                            if (!first) creditsJson.append(",");
                            first = false;
                            String dateStr = (row.getDateEcheance() == null) ? "" : row.getDateEcheance().toString();
                            creditsJson.append("{")
                                    .append("\"id\":").append(row.getRemboursementId()).append(",")
                                    .append("\"montant\":").append(BigDecimal.valueOf(mp)).append(",")
                                    .append("\"date\":\"").append(dateStr).append("\"")
                                    .append("}");
                        }
                    }
                }
                creditsJson.append("]");

                return "{"
                        + "\"budgetInitial\":" + budgetInitial + ","
                        + "\"budgetCourant\":" + budgetCourant + ","
                        + "\"budgetTotal\":" + budgetInitial + ","
                        + "\"totalInvesti\":" + totalInvesti + ","
                        + "\"budgetRestant\":" + budgetRestant + ","
                        + "\"creditedTotal\":" + creditedTotal + ","
                        + "\"credits\":" + creditsJson
                        + "}";

            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\"}";
            }
        }

        public String logout() {
            try {
                Utils.Session.setCurrentUser(null);
                Controllers.WebAuthController.openLoginOnNextLoad();
                javafx.application.Platform.runLater(() ->
                        Utils.sceneManager.switchTo("/web_auth.fxml", "Investia - Connexion")
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }
    }
}
