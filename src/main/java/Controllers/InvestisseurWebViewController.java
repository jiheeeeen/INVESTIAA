package Controllers;

import Entities.ProfilInvestisseur;
import Entities.ProfilEntrepreneur;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Entities.Investissement;
import Entities.Projet;
import Services.ProfilInvestisseurCRUD;
import Services.ProfilEntrepreneurCRUD;
import Services.UserCRUD;
import Services.InvestissementCRUD;
import Services.ChatbotProjectContextService;
import Services.GroqChatService;
import Utils.Session;
import Utils.WebViewBridgeUtil;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

public class InvestisseurWebViewController {
    private static volatile String investorPageOnNextLoad = null;

    public static void openInvestorPageOnNextLoad(String page) {
        investorPageOnNextLoad = page;
    }

    public static void openInvestorAccueil() {
        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Accueil Investisseur");
    }

    public static void openInvestorContact() {
        openInvestorPageOnNextLoad("contact_investisseur.html");
        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Contact entrepreneurs");
    }

    public static void openInvestorProjets() {
        sceneManager.switchTo("/investisseur_projets_view.fxml", "Investia - Projets");
    }

    public static void openInvestorMesInvestissements() {
        sceneManager.switchTo("/web/MesInvestissementsWebView.fxml", "Mes investissements");
    }

    public static void openInvestorProfil() {
        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil (Investisseur)");
    }

    public static void openInvestorEditProfil() {
        sceneManager.switchTo("/profil_investisseur_edit_view.fxml", "Investia - Modifier Profil");
    }

    public static void openInvestorWallet() {
        sceneManager.switchTo("/web/wallet_investisseur_view.fxml", "Investia - Mon Wallet");
    }

    public static void openInvestorFavoris() {
        openInvestorPageOnNextLoad("favoris_investisseur.html");
        sceneManager.switchTo("/investisseur_view.fxml", "Investia - Mes favoris");
    }

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

    // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ Ouvrir la page Projets Investisseur (FXML)
    public String openProjetsInvestisseur() {
        try {
            javafx.application.Platform.runLater(() ->
                    // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ IMPORTANT: ouvrir le mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªme ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cran projets que partout
                    openInvestorProjets()
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    @FXML private WebView webView;

    private final ProfilInvestisseurCRUD profilCrud = new ProfilInvestisseurCRUD();
    private final JavaBridge bridge = new JavaBridge();
    private FinancementBridgeController financementBridge;

    // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ bridge investissements (pour mes_investissements_view.html)
    private final JavaBridgeMesInvestissements investBridge = new JavaBridgeMesInvestissements();

    @FXML
    private void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ DEBUG (tu lÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢avais dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â )
                WebViewBridgeUtil.safeExec(engine, "console.log('[JAVA] Bridge injected by InvestisseurWebViewController');");

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ Injection CLEAN (javaBridge + alias + investBridge + __bridgeReady + anti-ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©crasement + callback)
                WebViewBridgeUtil.injectAll(engine, bridge, investBridge);
            }
        });

        URL html = resolveInvestisseurPage();
        if (html != null) engine.load(html.toExternalForm());
        else engine.loadContent("<h2>Erreur: page investisseur introuvable</h2>");
    }

    // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ Pages dans src/main/resources (racine)
    private URL resolveInvestisseurPage() {
        User current = Session.getCurrentUser();

        System.out.println("DEBUG INVEST VIEW: " + (current == null ? "null" :
                ("id=" + current.getId() + " role=" + current.getRole() +
                        " verif=" + current.getStatutVerification() + " active=" + current.isActive())));

        if (current == null) return getClass().getResource("/attenteValidation.html");

        if (current.getRole() != Role.INVESTISSEUR) {
            return getClass().getResource("/accueil.html");
        }

        if (current.getStatutVerification() == StatutVerification.VERIFIE && current.isActive()) {
            String requested = investorPageOnNextLoad;
            investorPageOnNextLoad = null;
            if (requested != null && !requested.isBlank()) {
                URL u = getClass().getResource("/" + requested);
                if (u != null) return u;
            }
            return getClass().getResource("/web/accueil_investisseur.html");
        }

        if (current.getStatutVerification() == StatutVerification.NON_VERIFIE) {
            return getClass().getResource("/completerInfos_investisseur.html");
        }

        return getClass().getResource("/attenteValidation.html");
    }

    private FinancementBridgeController getFinancementBridge() {
        if (financementBridge == null) {
            financementBridge = new FinancementBridgeController(new ProjetWebContext(webView));
        }
        return financementBridge;
    }

    // =========================================================
    // BRIDGE JS <-> JAVA
    // =========================================================
    public class JavaBridge {

        private final Services.ExchangeRateService exchangeRateService = new Services.ExchangeRateService();
        private final Services.GNewsService gnewsService = new Services.GNewsService();
        private final MessagerieBridgeController messagerieController = new MessagerieBridgeController();

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ CRUDs (tu les avais dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â )
        private final InvestissementCRUD investissementCRUD = new InvestissementCRUD();
        private final Services.ProjetCRUD projetCRUD = new Services.ProjetCRUD();
        private final ProfilEntrepreneurCRUD profilEntrepreneurCRUD = new ProfilEntrepreneurCRUD();
        private final UserCRUD userCRUD = new UserCRUD();

        public String getTndToEurUsdRatesJson() {
            try {
                return exchangeRateService.getTndToEurUsdRatesJson();
            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\"}";
            }
        }

        public String getFinanceNewsJson() {
            try {
                return gnewsService.getBusinessTopHeadlinesJson(6);
            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\",\"articles\":[]}";
            }
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
        public String getNativeLocationJson() { return getFinancementBridge().getNativeLocationJson(); }
        public String getIpInfoLocationJson() { return getFinancementBridge().getIpInfoLocationJson(); }
        public String startExternalGeoCaptureFromJs() { return getFinancementBridge().startExternalGeoCaptureFromJs(); }
        public String getExternalGeoCaptureStatusFromJs(String payload) { return getFinancementBridge().getExternalGeoCaptureStatusFromJs(payload); }
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
                javafx.application.Platform.runLater(() ->
                        openInvestorMesInvestissements()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String openContactInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                {
                    openInvestorContact();
                }
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String openFavorisInvestisseur() {
            try {
                javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorFavoris);
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // Permet a mes_investissements_view.html de charger les donnees
        // meme si la page est ouverte directement depuis l'accueil investisseur.
        public String getMesInvestissementsJson() {
            try {
                return investBridge.getMesInvestissementsJson();
            } catch (Exception e) {
                return "[]";
            }
        }

        public String getEntrepreneursContactParProjetInvestiJson() {
            try {
                User current = Session.getCurrentUser();
                if (current == null || current.getRole() != Role.INVESTISSEUR) return "[]";

                int investorKey;
                try {
                    investorKey = investissementCRUD.investorColumnIsUserId()
                            ? current.getId()
                            : profilCrud.getIdInvestisseurByUserId(current.getId());
                } catch (Exception ignored) {
                    investorKey = current.getId();
                }
                if (investorKey <= 0) return "[]";

                List<Investissement> investments = investissementCRUD.afficherParInvestisseur(investorKey);
                if (investments == null || investments.isEmpty()) return "[]";

                Set<Integer> investedProjectIds = new HashSet<>();
                for (Investissement inv : investments) {
                    if (inv != null && inv.getId_projet() > 0) investedProjectIds.add(inv.getId_projet());
                }
                if (investedProjectIds.isEmpty()) return "[]";

                List<Projet> allProjects = projetCRUD.afficher();
                if (allProjects == null || allProjects.isEmpty()) return "[]";

                StringBuilder sb = new StringBuilder("[");
                boolean firstProject = true;

                for (Projet p : allProjects) {
                    if (p == null || !investedProjectIds.contains(p.getIdProjet())) continue;

                    int entrepreneurUserId = 0;
                    String nom = "";
                    String email = "";
                    String telephone = "";

                    try {
                        // Cas 1: entrepreneur_id = id_entrepreneur
                        ProfilEntrepreneur pe = profilEntrepreneurCRUD.getById(p.getEntrepreneurId());
                        if (pe != null) entrepreneurUserId = pe.getIdUser();
                        // Cas 2: entrepreneur_id = id_user
                        if (entrepreneurUserId <= 0) {
                            pe = profilEntrepreneurCRUD.getByUserId(p.getEntrepreneurId());
                            if (pe != null) entrepreneurUserId = pe.getIdUser();
                            else entrepreneurUserId = p.getEntrepreneurId();
                        }

                        if (entrepreneurUserId > 0) {
                            User u = userCRUD.findById(entrepreneurUserId);
                            if (u != null) {
                                String n = u.getNom() == null ? "" : u.getNom().trim();
                                String pr = u.getPrenom() == null ? "" : u.getPrenom().trim();
                                nom = (n + " " + pr).trim();
                                email = u.getEmail() == null ? "" : u.getEmail().trim();
                                telephone = u.getTelephone() == null ? "" : u.getTelephone().trim();
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (nom.isBlank()) nom = "Entrepreneur #" + p.getEntrepreneurId();

                    if (!firstProject) sb.append(",");
                    firstProject = false;
                    sb.append("{")
                            .append("\"id_projet\":").append(p.getIdProjet()).append(",")
                            .append("\"titre\":").append(jsonString(p.getTitre())).append(",")
                            .append("\"entrepreneurs\":[{")
                            .append("\"id_user\":").append(entrepreneurUserId).append(",")
                            .append("\"nom\":").append(jsonString(nom)).append(",")
                            .append("\"email\":").append(jsonString(email)).append(",")
                            .append("\"telephone\":").append(jsonString(telephone))
                            .append("}]")
                            .append("}");
                }

                sb.append("]");
                return sb.toString();
            } catch (Exception e) {
                return "[]";
            }
        }      // =========================================================
        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ NEW : WALLET (AJOUT SANS SUPPRIMER TON TRAVAIL)
        // =========================================================
        public String openWalletInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorWallet()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // budget restant = budget_total - somme(investissements investisseur)
        // budget restant = budget_total - somme(investissements investisseur)
        public String getWalletStateJson() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "{\"error\":true,\"message\":\"USER_NOT_CONNECTED\"}";

                ProfilInvestisseur profil = profilCrud.getByUserId(u.getId());
                if (profil == null || profil.getBudgetTotal() == null) {
                    return "{\"error\":true,\"message\":\"PROFIL_OR_BUDGET_TOTAL_MISSING\"}";
                }

                BigDecimal budgetTotal = profil.getBudgetTotal();

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ IMPORTANT: si la colonne investissement est user_id => on somme par userId
                // sinon => on somme par idInvestisseur (profil.getIdInvestisseur())
                int keyForSum;
                try {
                    boolean isUserId = investissementCRUD.investorColumnIsUserId();
                    keyForSum = isUserId ? u.getId() : profil.getIdInvestisseur();
                } catch (Exception ex) {
                    // fallback safe
                    keyForSum = profil.getIdInvestisseur();
                }

                double sum = investissementCRUD.sumMontantParInvestisseur(keyForSum);
                BigDecimal totalInvesti = BigDecimal.valueOf(sum);

                BigDecimal budgetRestant = budgetTotal.subtract(totalInvesti);
                if (budgetRestant.compareTo(BigDecimal.ZERO) < 0) budgetRestant = BigDecimal.ZERO;

                return "{"
                        + "\"budgetTotal\":" + budgetTotal + ","
                        + "\"totalInvesti\":" + totalInvesti + ","
                        + "\"budgetRestant\":" + budgetRestant
                        + "}";

            } catch (Exception e) {
                String msg = (e.getMessage() == null ? "UNKNOWN" : e.getMessage()).replace("\"", "'");
                return "{\"error\":true,\"message\":\"" + msg + "\"}";
            }
        }

        // =========================================================
        // EXISTANT (ton travail)
        // =========================================================

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ Ouvrir ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cran investissement (FXML) pour le projet sÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lectionnÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©
        public String openInvestir(String idProjet) {
            try {
                if (idProjet == null || idProjet.trim().isEmpty()) return "ERROR:ID_PROJET_REQUIRED";

                int pid = Integer.parseInt(idProjet.trim());

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ stocker l'id projet pour l'ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cran suivant
                Session.setSelectedProjetId(pid);

                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/investissement_form.fxml", "Investia - Investir")
                );

                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ utilisÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© par investissement_form.html (header)
        public String getSelectedProjetId() {
            try {
                int pid = Session.getSelectedProjetId();
                return pid <= 0 ? "" : String.valueOf(pid);
            } catch (Exception e) {
                return "";
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ utilisÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© par investissement_form.html (username)
        public String getCurrentUserName() {
            User u = Session.getCurrentUser();
            if (u == null) return "";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            if (!full.isEmpty()) return full;
            return u.getEmail() == null ? "" : u.getEmail().trim();
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ utilisÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© par investissement_form.html (table)
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
                    out.append("\"id_investisseur\":").append(x.getId_investisseur());
                    out.append("}");
                }
                out.append("]");
                return out.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ ANCIENNE mÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©thode (NE PAS SUPPRIMER)
        public String createInvestissement(String montant) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";

                String pidStr = getSelectedProjetId();
                if (pidStr == null || pidStr.isBlank()) return "ERROR:ID_PROJET_REQUIRED";

                if (montant == null || montant.trim().isEmpty()) return "ERROR:MONTANT_REQUIRED";

                double m = Double.parseDouble(montant.trim());
                if (m <= 0) return "ERROR:MONTANT_INVALID";

                int pid = Integer.parseInt(pidStr);

                ProfilInvestisseur profil = profilCrud.getByUserId(u.getId());
                if (profil == null) return "ERROR:PROFIL_INVESTISSEUR_NOT_FOUND";
                int idInvestisseur = profil.getIdInvestisseur();

                Investissement inv = new Investissement();
                inv.setMontant(m);
                inv.setDate_investissement(new Date(System.currentTimeMillis()));
                inv.setId_investisseur(idInvestisseur);
                inv.setId_projet(pid);

                investissementCRUD.ajouter(inv);
                return "OK:" + inv.getId_investissement();
            } catch (NumberFormatException e) {
                return "ERROR:INVALID_NUMBER";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ surcharge compatible HTML (4 params)
        public String createInvestissement(String montant, String taux, String duree, String statut) {
            try {
                if (taux == null || taux.trim().isEmpty()) taux = "0";
                if (duree == null || duree.trim().isEmpty()) duree = "1";
                if (statut == null || statut.trim().isEmpty()) statut = "EN_ATTENTE";

                Double.parseDouble(taux.trim());
                Integer.parseInt(duree.trim());

                return createInvestissement(montant);
            } catch (NumberFormatException ex) {
                return "ERROR:INVALID_NUMBER";
            }
        }

        public String backToProjets() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorProjets()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String openProjetsInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorProjets()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String goAccueilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorAccueil()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ LISTE PROJETS pour investisseur (appelÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e par projet_view_investisseur.html)
        public String listProjets() {
            try {
                java.sql.Connection c = Utils.MyBD.getInstance().getConn();

                String sql = """
                    SELECT id_projet, statut, titre, secteur, description_courte, objectif_tnd, updated_at
                    FROM projet
                    ORDER BY COALESCE(updated_at, created_at) DESC
                """;

                java.sql.PreparedStatement ps = c.prepareStatement(sql);
                java.sql.ResultSet rs = ps.executeQuery();

                StringBuilder out = new StringBuilder();
                out.append("[");

                boolean first = true;
                while (rs.next()) {
                    if (!first) out.append(",");
                    first = false;

                    int id = rs.getInt("id_projet");
                    String statutDb = rs.getString("statut");
                    String titre = rs.getString("titre");
                    String secteur = rs.getString("secteur");
                    String desc = rs.getString("description_courte");
                    BigDecimal objectif = rs.getBigDecimal("objectif_tnd");
                    java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");

                    String statusUi = mapStatutToUi(statutDb);

                    out.append("{");
                    out.append("\"id\":").append(id).append(",");
                    out.append("\"status\":").append(json(statusUi)).append(",");
                    out.append("\"title\":").append(json(titre)).append(",");
                    out.append("\"category\":").append(json(secteur)).append(",");
                    out.append("\"short\":").append(json(desc)).append(",");
                    out.append("\"goal\":").append(objectif == null ? "0" : objectif.toPlainString()).append(",");
                    out.append("\"updatedAt\":").append(json(updatedAt == null ? "" : updatedAt.toString()));
                    out.append("}");
                }

                out.append("]");

                rs.close();
                ps.close();

                return out.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "[]";
            }
        }

        // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©tails projet
        public String getProjetById(String id) {
            try {
                int projectId = Integer.parseInt(id);
                Entities.Projet p = projetCRUD.getById(projectId);
                if (p == null) return "null";
                return ProjetWebUtils.toDetailJson(p);
            } catch (Exception e) {
                return "null";
            }
        }

        private String mapStatutToUi(String db) {
            if (db == null) return "DRAFT";
            String s = db.trim().toUpperCase();

            if (s.equals("BROUILLON")) return "DRAFT";
            if (s.equals("EN_ATTENTE")) return "PENDING";
            if (s.equals("VALIDE")) return "VALIDATED";
            if (s.equals("REFUSE")) return "REJECTED";

            if (s.equals("DRAFT") || s.equals("PENDING") || s.equals("VALIDATED") || s.equals("REJECTED")) return s;

            return "DRAFT";
        }

        private String json(String s) {
            if (s == null) return "null";
            String esc = s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            return "\"" + esc + "\"";
        }

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorProfil()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String openEditProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        openInvestorEditProfil()
                );
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String getCurrentUserId() {
            User u = Session.getCurrentUser();
            return u == null ? "" : String.valueOf(u.getId());
        }

        public String getCurrentUserRole() {
            User u = Session.getCurrentUser();
            return (u == null || u.getRole() == null) ? "" : u.getRole().name();
        }

        public String getProfilInvestisseurJson() {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "null";

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    sb.append("\"idUser\":").append(u.getId()).append(",");
                    sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
                    sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
                    sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
                    sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
                    sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");
                    sb.append("\"budgetTotal\":null,");
                    sb.append("\"budgetMensuel\":null,");
                    sb.append("\"ticketMoyenParProjet\":null,");
                    sb.append("\"horizonInvestissement\":\"MOYEN\",");
                    sb.append("\"bio\":null,");
                    sb.append("\"secteurs\":null,");
                    sb.append("\"cinRectoUrl\":null,");
                    sb.append("\"cinVersoUrl\":null,");
                    sb.append("\"photoUrl\":null");
                    sb.append("}");
                    return sb.toString();
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"idUser\":").append(u.getId()).append(",");

                sb.append("\"nom\":").append(jsonString(u.getNom())).append(",");
                sb.append("\"prenom\":").append(jsonString(u.getPrenom())).append(",");
                sb.append("\"email\":").append(jsonString(u.getEmail())).append(",");
                sb.append("\"telephone\":").append(jsonString(u.getTelephone())).append(",");
                sb.append("\"cin\":").append(jsonString(u.getCin())).append(",");

                sb.append("\"budgetTotal\":").append(p.getBudgetTotal() == null ? "null" : p.getBudgetTotal()).append(",");
                sb.append("\"budgetMensuel\":").append(p.getBudgetMensuel() == null ? "null" : p.getBudgetMensuel()).append(",");
                sb.append("\"ticketMoyenParProjet\":").append(p.getTicketMoyenParProjet() == null ? "null" : p.getTicketMoyenParProjet()).append(",");
                sb.append("\"horizonInvestissement\":").append(jsonString(p.getHorizonInvestissement())).append(",");
                sb.append("\"bio\":").append(jsonString(p.getBio())).append(",");
                sb.append("\"secteurs\":").append(jsonString(p.getSecteurs() == null ? null : String.join(",", p.getSecteurs()))).append(",");
                sb.append("\"cinRectoUrl\":").append(jsonString(p.getCinRectoUrl())).append(",");
                sb.append("\"cinVersoUrl\":").append(jsonString(p.getCinVersoUrl())).append(",");
                sb.append("\"photoUrl\":").append(jsonString(p.getPhotoUrl()));
                sb.append("}");

                return sb.toString();
            } catch (Exception e) {
                return "null";
            }
        }

        public String updateProfilInvestisseur(
                String nom,
                String prenom,
                String email,
                String telephone,
                String cin,
                String budgetTotal,
                String budgetMensuel,
                String ticketMoyenParProjet,
                String horizonInvestissement,
                String bio,
                String secteursCsv
        ) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";
                if (u.getRole() != Role.INVESTISSEUR) return "ERROR:ROLE_NOT_ALLOWED";

                if (isBlank(email)) return "ERROR:EMAIL_REQUIRED";
                if (isBlank(ticketMoyenParProjet)) return "ERROR:TICKET_REQUIRED";
                if (isBlank(budgetTotal)) return "ERROR:BUDGET_TOTAL_REQUIRED";
                if (isBlank(horizonInvestissement)) horizonInvestissement = "MOYEN";

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ VALIDATION budgets/ticket (AJOUT)
                BigDecimal bt;
                BigDecimal bm;
                BigDecimal tk;
                try {
                    bt = parseRequiredPositiveBD(budgetTotal, "ERROR:BUDGET_TOTAL_REQUIRED");
                    bm = parseOptionalPositiveBD(budgetMensuel);
                    tk = parseRequiredPositiveBD(ticketMoyenParProjet, "ERROR:TICKET_REQUIRED");
                    validateBudgetsAndTicket(bt, bm, tk);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                u.setNom(emptyToNull(nom));
                u.setPrenom(emptyToNull(prenom));
                u.setEmail(email.trim());
                u.setTelephone(emptyToNull(telephone));
                u.setCin(emptyToNull(cin));

                UserCRUD userCrud = new UserCRUD();
                userCrud.updateUser(u);
                Session.setCurrentUser(u);

                ProfilInvestisseur p = profilCrud.getByUserId(u.getId());
                if (p == null) {
                    p = new ProfilInvestisseur();
                    p.setIdUser(u.getId());
                }

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ appliquer les valeurs validÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©es
                p.setBudgetTotal(bt);
                p.setBudgetMensuel(bm);
                p.setTicketMoyenParProjet(tk);

                p.setHorizonInvestissement(horizonInvestissement.trim());
                p.setBio(emptyToNull(bio));
                p.setSecteurs(parseSecteurs(secteursCsv));

                profilCrud.upsertForUser(p);

                javafx.application.Platform.runLater(() ->
                        openInvestorProfil()
                );

                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String saveProfilInvestisseur(String budgetTotal,
                                             String budgetMensuel,
                                             String ticketMoyenParProjet,
                                             String horizonInvestissement,
                                             String bio,
                                             String accepteConditions,
                                             String secteursCsv,
                                             String cinRectoUrl,
                                             String cinVersoUrl,
                                             String photoUrl) {
            try {
                User u = Session.getCurrentUser();
                if (u == null) return "ERROR:USER_NOT_CONNECTED";
                if (u.getRole() != Role.INVESTISSEUR) return "ERROR:ROLE_NOT_ALLOWED";

                if (budgetTotal == null || budgetTotal.trim().isEmpty()) return "ERROR:budget_total_required";
                if (ticketMoyenParProjet == null || ticketMoyenParProjet.trim().isEmpty()) return "ERROR:ticket_required";
                if (horizonInvestissement == null || horizonInvestissement.trim().isEmpty()) return "ERROR:horizon_required";
                if (!parseBoolean(accepteConditions)) return "ERROR:must_accept_conditions";

                if (cinRectoUrl == null || cinRectoUrl.trim().isEmpty()) return "ERROR:CIN_RECTO_REQUIRED";
                if (cinVersoUrl == null || cinVersoUrl.trim().isEmpty()) return "ERROR:CIN_VERSO_REQUIRED";
                if (photoUrl == null || photoUrl.trim().isEmpty()) return "ERROR:PHOTO_REQUIRED";

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ VALIDATION budgets/ticket (AJOUT)
                BigDecimal bt;
                BigDecimal bm;
                BigDecimal tk;
                try {
                    bt = parseRequiredPositiveBD(budgetTotal, "ERROR:budget_total_required");
                    bm = parseOptionalPositiveBD(budgetMensuel);
                    tk = parseRequiredPositiveBD(ticketMoyenParProjet, "ERROR:ticket_required");
                    validateBudgetsAndTicket(bt, bm, tk);
                } catch (IllegalArgumentException ex) {
                    return ex.getMessage();
                }

                ProfilInvestisseur p = new ProfilInvestisseur();
                p.setIdUser(u.getId());

                // ÃƒÆ’Ã‚Â¢Ãƒâ€¦Ã¢â‚¬Å“ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ appliquer les valeurs validÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©es
                p.setBudgetTotal(bt);
                p.setBudgetMensuel(bm);
                p.setTicketMoyenParProjet(tk);

                p.setHorizonInvestissement(horizonInvestissement.trim());
                p.setBio(emptyToNull(bio));
                p.setAccepteConditions(true);
                p.setSecteurs(parseSecteurs(secteursCsv));

                p.setCinRectoUrl(cinRectoUrl.trim());
                p.setCinVersoUrl(cinVersoUrl.trim());
                p.setPhotoUrl(photoUrl.trim());

                profilCrud.upsertForUser(p);

                UserCRUD userCRUD = new UserCRUD();
                userCRUD.submitProfileForVerification(u.getId());
                u.setStatutVerification(StatutVerification.EN_ATTENTE);
                u.setActive(false);
                Session.setCurrentUser(u);

                javafx.application.Platform.runLater(() -> {
                    WebEngine engine2 = webView.getEngine();
                    URL page = getClass().getResource("/attenteValidation.html");
                    if (page != null) engine2.load(page.toExternalForm());
                    else engine2.loadContent("<h2>attenteValidation.html introuvable (racine resources)</h2>");
                });

                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String uploadPdf(String docType) {
            try {
                User current = Session.getCurrentUser();
                if (current == null) return "ERROR:USER_NOT_CONNECTED";

                FileChooser chooser = new FileChooser();
                chooser.setTitle("Charger un PDF");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

                Window owner = (webView != null && webView.getScene() != null) ? webView.getScene().getWindow() : null;
                File selected = chooser.showOpenDialog(owner);
                if (selected == null) return "";

                String lower = selected.getName().toLowerCase();
                if (!lower.endsWith(".pdf")) return "ERROR:ONLY_PDF";

                Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "investisseurs", String.valueOf(current.getId()));
                Files.createDirectories(uploadDir);

                String safe = sanitizeDocType(docType);
                String filename = safe + "_" + System.currentTimeMillis() + ".pdf";
                Path dest = uploadDir.resolve(filename);

                Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                return dest.toUri().toString();
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        public String uploadImage(String docType) {
            try {
                User current = Session.getCurrentUser();
                if (current == null) return "ERROR:USER_NOT_CONNECTED";

                FileChooser chooser = new FileChooser();
                chooser.setTitle("Charger une image");
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
                );

                Window owner = (webView != null && webView.getScene() != null) ? webView.getScene().getWindow() : null;
                File selected = chooser.showOpenDialog(owner);
                if (selected == null) return "";

                String lower = selected.getName().toLowerCase();
                String ext;
                if (lower.endsWith(".png")) ext = ".png";
                else if (lower.endsWith(".jpg")) ext = ".jpg";
                else if (lower.endsWith(".jpeg")) ext = ".jpeg";
                else if (lower.endsWith(".webp")) ext = ".webp";
                else return "ERROR:ONLY_IMAGE";

                Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads", "investisseurs",
                        String.valueOf(current.getId()), "images");
                Files.createDirectories(uploadDir);

                String safe = sanitizeDocType(docType);
                String filename = safe + "_" + System.currentTimeMillis() + ext;
                Path dest = uploadDir.resolve(filename);

                Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                return dest.toUri().toString();
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

    // =========================================================
    // HELPERS
    // =========================================================
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
    }

    private static Set<String> parseSecteurs(String csv) {
        if (csv == null || csv.trim().isEmpty()) return null;
        Set<String> set = new HashSet<>();
        for (String s : csv.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return set.isEmpty() ? null : set;
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    private static String sanitizeDocType(String input) {
        if (input == null || input.isBlank()) return "document";
        String out = input.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
        return out.isBlank() ? "document" : out;
    }

    private static BigDecimal parseRequiredPositiveBD(String value, String errorCode) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(errorCode);
        BigDecimal bd = new BigDecimal(value.trim());
        if (bd.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(errorCode);
        return bd;
    }

    private static BigDecimal parseOptionalPositiveBD(String value) {
        if (value == null) return null;
        String t = value.trim();
        if (t.isEmpty()) return null;

        BigDecimal bd = new BigDecimal(t);
        if (bd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ERROR:BUDGET_MENSUEL_INVALID");
        }
        return bd;
    }

    private static void validateBudgetsAndTicket(BigDecimal budgetTotal, BigDecimal budgetMensuel, BigDecimal ticket) {
        if (budgetMensuel != null && budgetTotal.compareTo(budgetMensuel) <= 0) {
            throw new IllegalArgumentException("ERROR:BUDGET_TOTAL_MUST_BE_GREATER_THAN_MONTHLY");
        }

        if (ticket.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("ERROR:TICKET_MAX_10000");
        }

        if (ticket.compareTo(budgetTotal) > 0) {
            throw new IllegalArgumentException("ERROR:TICKET_MUST_BE_LESS_OR_EQUAL_BUDGET_TOTAL");
        }

        if (budgetMensuel != null && ticket.compareTo(budgetMensuel) > 0) {
            throw new IllegalArgumentException("ERROR:TICKET_MUST_BE_LESS_OR_EQUAL_BUDGET_MENSUEL");
        }
    }
}




