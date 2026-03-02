package Controllers;

import Entities.Projet;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Services.ChatbotProjectContextService;
import Services.GroqChatService;
import Services.ProjetCRUD;
import Utils.Session;
import Utils.WebViewBridgeUtil;
import Utils.sceneManager;
import Controllers.WebAuthController;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

public class InvestisseurProjetsWebViewController {

    @FXML
    private WebView webView;

    private final ProjetCRUD crud = new ProjetCRUD();
    private final JavaBridge bridge = new JavaBridge();
    private FinancementBridgeController financementBridge;

    private static String resolveGroqApiKey() {
        String fromEnv = System.getenv("GROQ_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();

        String fromProperty = System.getProperty("GROQ_API_KEY");
        if (fromProperty != null && !fromProperty.isBlank()) return fromProperty.trim();

        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        Path current = Path.of("").toAbsolutePath().normalize();
        candidates.add(current.resolve(".env"));
        candidates.add(current.resolve("..").resolve(".env").normalize());
        candidates.add(current.resolve("..").resolve("..").resolve(".env").normalize());
        candidates.add(Path.of(System.getProperty("user.home"), ".env"));

        for (Path dotEnv : candidates) {
            if (!Files.exists(dotEnv)) continue;
            try {
                for (String rawLine : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
                    String line = rawLine == null ? "" : rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) continue;
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    if (!"GROQ_API_KEY".equals(key)) continue;
                    String value = line.substring(idx + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value.isBlank() ? null : value;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    @FXML
    private void initialize() {
        System.out.println("Ã¢Å“â€¦ OPEN: InvestisseurProjetsWebViewController");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                // Ã¢Å“â€¦ Debug (tu lÃ¢â‚¬â„¢avais)
                WebViewBridgeUtil.safeExec(engine, "console.log('[JAVA] Bridge injected by InvestisseurProjetsWebViewController');");

                // Ã¢Å“â€¦ Injection CLEAN (javaBridge + alias + __bridgeReady + anti-ÃƒÂ©crasement + callback)
                // Ã¢Å¡Â Ã¯Â¸Â Ici pas besoin de investBridge sÃƒÂ©parÃƒÂ©, ton bridge suffit
                WebViewBridgeUtil.injectAll(engine, bridge, bridge);
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

    private FinancementBridgeController getFinancementBridge() {
        if (financementBridge == null) {
            financementBridge = new FinancementBridgeController(new ProjetWebContext(webView));
        }
        return financementBridge;
    }

    // ==========================
    // BRIDGE
    // ==========================
    public class JavaBridge {
        private final Services.GNewsService gnewsService = new Services.GNewsService();
        private final MessagerieBridgeController messagerieController = new MessagerieBridgeController();
        public String getFinanceNewsJson() {
            try {
                return gnewsService.getBusinessTopHeadlinesJson(6);
            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\",\"articles\":[]}";
            }
        }

        public String getCurrentUserId() {
            User u = Session.getCurrentUser();
            return u == null ? "" : String.valueOf(u.getId());
        }

        public String setSelectedContactUserId(String id) { return messagerieController.setSelectedContactUserId(id); }
        public String getSelectedContactUserId() { return messagerieController.getSelectedContactUserId(); }
        public String getMessagerieContactsJson() { return messagerieController.getMessagerieContactsJson(); }
        public String getConversationMessagesJson(String otherUserId) { return messagerieController.getConversationMessagesJson(otherUserId); }
        public String sendMessageToUser(String otherUserId, String contenu) { return messagerieController.sendMessageToUser(otherUserId, contenu); }
        public String getUserSummaryJson(String userId) { return messagerieController.getUserSummaryJson(userId); }
        public String getUnreadMessagesCount() { return messagerieController.getUnreadMessagesCount(); }
        public String askChatbot(String userMessage) {
            try {
                String cleanMessage = userMessage == null ? "" : userMessage.trim();
                if (cleanMessage.isEmpty()) return "Pose-moi une question et je te reponds.";

                User currentUser = Session.getCurrentUser();
                ChatbotProjectContextService contextService = new ChatbotProjectContextService();
                String quickAnswer = contextService.tryQuickAnswer(currentUser, cleanMessage);
                if (quickAnswer != null && !quickAnswer.isBlank()) return quickAnswer;

                String apiKey = resolveGroqApiKey();
                if (apiKey == null || apiKey.isBlank()) {
                    return "Je peux repondre aux questions sur vos projets, investissements et financements. "
                            + "Pour des reponses IA plus detaillees, configurez GROQ_API_KEY.";
                }

                GroqChatService chatService = new GroqChatService(apiKey, "openai/gpt-oss-120b");
                String context = contextService.buildContext(currentUser);
                String systemInstruction =
                        "Tu es l'assistant IA d'Investia. "
                                + "Reponds uniquement en francais. "
                                + "Base tes reponses sur le contexte metier fourni (base de donnees de l'utilisateur connecte). "
                                + "Si une information manque dans le contexte, dis-le explicitement et propose l'action a faire dans l'application.";
                return chatService.chatWithContext(systemInstruction, context, cleanMessage);
            } catch (Exception e) {
                return "ERROR: " + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // --- Financement / Remboursement ---
        public String getProjectsJson() { return getFinancementBridge().getProjectsJson(); }
        public String getMyProjectsJson() { return getFinancementBridge().getMyProjectsJson(); }
        public String getProjectDashboardJson(int projectId) { return getFinancementBridge().getProjectDashboardJson(projectId); }
        public String getTndToEurUsdRatesJson() { return getFinancementBridge().getTndToEurUsdRatesJson(); }
        public String getInvestissementsJson() { return getFinancementBridge().getInvestissementsJson(); }
        public String getCurrentInvestorInvestissementsByProjectJson() { return getFinancementBridge().getCurrentInvestorInvestissementsByProjectJson(); }
        public String getFinancementsByProjectJson(int projectId) { return getFinancementBridge().getFinancementsByProjectJson(projectId); }
        public String getFinancementsByProjectWithRembStatusJson(int projectId) { return getFinancementBridge().getFinancementsByProjectWithRembStatusJson(projectId); }
        public String getFinancementRiskCheckJson(int projectId, int investId, double amount) { return getFinancementBridge().getFinancementRiskCheckJson(projectId, investId, amount); }
        public String getSmartRepaymentPlanJson(double amount, double ratePct) { return getFinancementBridge().getSmartRepaymentPlanJson(amount, ratePct); }
        public String getFinancementsJson() { return getFinancementBridge().getFinancementsJson(); }
        public String getFinancementsForCurrentInvestorJson() { return getFinancementBridge().getFinancementsForCurrentInvestorJson(); }
        public String getFinancementByIdJson(int id) { return getFinancementBridge().getFinancementByIdJson(id); }
        public String createFinancementFromJs(String payload) { return getFinancementBridge().createFinancementFromJs(payload); }
        public String updateFinancementFromJs(String payload) { return getFinancementBridge().updateFinancementFromJs(payload); }
        public String deleteFinancementFromJs(String payload) { return getFinancementBridge().deleteFinancementFromJs(payload); }
        public String deleteFinancement(int id) { return getFinancementBridge().deleteFinancement(id); }
        public String getRemboursementsJson() { return getFinancementBridge().getRemboursementsJson(); }
        public String getRemboursementsByProjectJson(int projectId) { return getFinancementBridge().getRemboursementsByProjectJson(projectId); }
        public String getRemboursementsForCurrentInvestorJson() { return getFinancementBridge().getRemboursementsForCurrentInvestorJson(); }
        public String getRemboursementsByFinancementJson(int financementId) { return getFinancementBridge().getRemboursementsByFinancementJson(financementId); }
        public String getAuditLogsJsonFromJs(String payload) { return getFinancementBridge().getAuditLogsJsonFromJs(payload); }
        public String getRemboursementByIdJson(int id) { return getFinancementBridge().getRemboursementByIdJson(id); }
        public String createRemboursementFromJs(String payload) { return getFinancementBridge().createRemboursementFromJs(payload); }
        public String updateRemboursementFromJs(String payload) { return getFinancementBridge().updateRemboursementFromJs(payload); }
        public String deleteRemboursementFromJs(String payload) { return getFinancementBridge().deleteRemboursementFromJs(payload); }
        public String payRemboursementFromJs(String payload) { return getFinancementBridge().payRemboursementFromJs(payload); }
        public String getPaymentStatusFromJs(String payload) { return getFinancementBridge().getPaymentStatusFromJs(payload); }
        public String createStripeCheckoutSessionFromJs(String payload) { return getFinancementBridge().createStripeCheckoutSessionFromJs(payload); }
        public String confirmStripeCheckoutPaymentFromJs(String payload) { return getFinancementBridge().confirmStripeCheckoutPaymentFromJs(payload); }
        public String openExternalUrl(String payload) { return getFinancementBridge().openExternalUrl(payload); }
        public String openReceiptDocumentFromJs(String payload) { return getFinancementBridge().openReceiptDocumentFromJs(payload); }
        public String openStripeCheckoutPopup(String payload) { return getFinancementBridge().openStripeCheckoutPopup(payload); }
        public String createSignatureForPaymentFromJs(String payload) { return getFinancementBridge().createSignatureForPaymentFromJs(payload); }
        public String downloadPaymentReportFromJs(String payload) { return getFinancementBridge().downloadPaymentReportFromJs(payload); }
        public String printFileFromJs(String payload) { return getFinancementBridge().printFileFromJs(payload); }
        public String getPaymentDocumentsForCurrentEntrepreneurJson() { return getFinancementBridge().getPaymentDocumentsForCurrentEntrepreneurJson(); }
        public String getPaymentDocumentsByFinancementJson(int financementId) { return getFinancementBridge().getPaymentDocumentsByFinancementJson(financementId); }
        public String verifyReceiptIntegrityFromJs(String payload) { return getFinancementBridge().verifyReceiptIntegrityFromJs(payload); }
        public String getPaymentDocumentsByRemboursementsFromJs(String payload) { return getFinancementBridge().getPaymentDocumentsByRemboursementsFromJs(payload); }
        public String getRemboursementCalendarForCurrentEntrepreneurJson() { return getFinancementBridge().getRemboursementCalendarForCurrentEntrepreneurJson(); }
        public String syncRemboursementCalendarToMicrosoftFromJs() { return getFinancementBridge().syncRemboursementCalendarToMicrosoftFromJs(); }
        public String getMicrosoftCalendarEventsFromJs(String payload) { return getFinancementBridge().getMicrosoftCalendarEventsFromJs(payload); }
        public String getCalendarSyncDiagnosticsFromJs() { return getFinancementBridge().getCalendarSyncDiagnosticsFromJs(); }
        public String getEntrepreneurNotesJson() { return getFinancementBridge().getEntrepreneurNotesJson(); }
        public String createEntrepreneurNoteFromJs(String payload) { return getFinancementBridge().createEntrepreneurNoteFromJs(payload); }
        public String updateEntrepreneurNoteFromJs(String payload) { return getFinancementBridge().updateEntrepreneurNoteFromJs(payload); }
        public String deleteEntrepreneurNoteFromJs(String payload) { return getFinancementBridge().deleteEntrepreneurNoteFromJs(payload); }
        public String getEntrepreneurCalcHistoryJson() { return getFinancementBridge().getEntrepreneurCalcHistoryJson(); }
        public String saveEntrepreneurCalcFromJs(String payload) { return getFinancementBridge().saveEntrepreneurCalcFromJs(payload); }
        public String deleteEntrepreneurCalcFromJs(String payload) { return getFinancementBridge().deleteEntrepreneurCalcFromJs(payload); }

        public String openMesInvestissements() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorMesInvestissements);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String openContactInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorContact);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // Ã¢Å“â€¦ navbar : ouvrir EDIT profil investisseur (UNE SEULE FOIS)
        public String openEditProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorEditProfil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // Ã¢Å“â€¦ navbar : refresh (UNE SEULE FOIS)
        public String refreshProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProjets);
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

        // Ã¢Å“â€¦ appelÃƒÂ© par projet_view_investisseur.html
        public String listProjets() {
            try {
                List<Projet> list = crud.afficher();
                StringBuilder sb = new StringBuilder();
                sb.append("[");

                boolean first = true;
                for (Projet p : list) {

                    // Ã¢Å“â€¦ garder seulement les projets VALIDÃƒâ€°S
                    String uiStatus = mapStatusForUi(p.getStatut());
                    if (!"VALIDATED".equals(uiStatus)
                            && !"PROJECT_IN_PROGRESS".equals(uiStatus)
                            && !"INVESTMENT_OPEN".equals(uiStatus)) {
                        continue;
                    }

                    if (!first) sb.append(",");
                    sb.append(toListJson(p));
                    first = false;
                }

                sb.append("]");
                return sb.toString();
            } catch (SQLException e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // Ã¢Å“â€¦ dÃƒÂ©tails projet pour detailsInvestisseur.html
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

        // Ã¢Å“â€¦ IMPORTANT : ouvrir l'ÃƒÂ©cran Investir via JavaFX (met Session.selectedProjetId)
        public String openInvestir(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";

                int pid = Integer.parseInt(idProjet.trim());
                if (pid <= 0) return "ERROR:ID_PROJET_INVALID";

                // Ã¢Å“â€¦ stocker l'id projet pour l'ÃƒÂ©cran investissement
                Session.setSelectedProjetId(pid);

                // Ã¢Å“â€¦ Ouvrir le bon ÃƒÂ©cran: celui qui contient le WebView investissement
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investissement_view.fxml", "Investia - Investir")
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // Ã¢Å“â€¦ navbar : Accueil
        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorAccueil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // Ã¢Å“â€¦ navbar : Projets
        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProjets);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // Ã¢Å“â€¦ navbar : ouvrir profil investisseur
        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProfil);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // Ã¢Å“â€¦ AJOUT MINIMAL : WALLET (pour navbar)
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

        // Ã¢Å“â€¦ AJOUT MINIMAL : alias (si navbar appelle openWallet)
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

        String s = statut.trim().toUpperCase();

        switch (s) {
            // FR
            case "BROUILLON": return "DRAFT";
            case "EN_ATTENTE": return "PENDING";
            case "VALIDE": return "VALIDATED";
            case "REFUSE": return "REJECTED";
            case "PROJET_EN_COURS": return "PROJECT_IN_PROGRESS";
            case "INVESTISSEMENT_EN_COURS": return "INVESTMENT_OPEN";
            case "EN_COURS": return "INVESTMENT_OPEN";

            // EN
            case "DRAFT": return "DRAFT";
            case "PENDING": return "PENDING";
            case "VALIDATED": return "VALIDATED";
            case "REJECTED": return "REJECTED";
            case "PROJECT_IN_PROGRESS": return "PROJECT_IN_PROGRESS";
            case "INVESTMENT_OPEN": return "INVESTMENT_OPEN";

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


