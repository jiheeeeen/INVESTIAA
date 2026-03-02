package org.example;

import Controllers.AnnulationBridgeController;
import Controllers.FinancementBridgeController;
import Controllers.ProfilBridgeController;
import Controllers.ProjetBridgeController;
import Controllers.ProjetWebContext;
import Controllers.SessionBridgeController;
import Controllers.MessagerieBridgeController;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Services.ChatbotProjectContextService;
import Services.GroqChatService;
import Utils.MyBD;
import Utils.Session;
import Utils.sceneManager;
import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class ProjetWebViewController {
    private static volatile String entrepreneurPageOnNextLoad;

    public static void openEntrepreneurPageOnNextLoad(String page) {
        entrepreneurPageOnNextLoad = page;
    }

    @FXML
    private WebView webView;

    private JavaBridge bridge;

    @FXML
    private void initialize() {
        if (webView == null) {
            return;
        }
        bridge = new JavaBridge(webView);
        WebEngine engine = webView.getEngine();
        if (engine == null) {
            return;
        }
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    Object winObj = engine.executeScript("window");
                    if (winObj instanceof JSObject window) {
                        window.setMember("javaBridge", bridge);
                        engine.executeScript("window.__bridgeReady = true;");
                    }
                } catch (Throwable ignored) {
                }
            }
        });

        URL html = resolveLandingPage();
        if (html != null) {
            engine.load(html.toExternalForm());
        } else {
            engine.loadContent("<h2>Erreur: projet_view.html introuvable</h2>");
        }
    }

    private URL resolveLandingPage() {
        User current = Session.getCurrentUser();
        if (current == null) return getClass().getResource("/attenteValidation.html");
        if (current.getRole() == Role.ADMIN) return getClass().getResource("/accueil.html");

        if (current.getStatutVerification() == StatutVerification.VERIFIE && current.isActive()) {
            if (current.getRole() == Role.INVESTISSEUR) {
                return getClass().getResource("/web/accueil_investisseur.html");
            }
            String requested = entrepreneurPageOnNextLoad;
            entrepreneurPageOnNextLoad = null;
            if (requested != null && !requested.isBlank()) {
                String normalized = requested.startsWith("/") ? requested : ("/" + requested);
                URL target = getClass().getResource(normalized);
                if (target != null) return target;
            }
            return getClass().getResource("/accueil.html");
        }

        if (current.getStatutVerification() == StatutVerification.NON_VERIFIE
                && current.getRole() == Role.ENTREPRENEUR) {
            return getClass().getResource("/completerInfos.html");
        }

        return getClass().getResource("/attenteValidation.html");
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

    public class JavaBridge {
        private final WebView hostWebView;
        private Stage calendarStage;
        private WebEngine calendarEngine;
        private final SessionBridgeController sessionController;
        private final ProjetBridgeController projetController;
        private final AnnulationBridgeController annulationController;
        private final ProfilBridgeController profilController;
        private final FinancementBridgeController financementController;
        private final MessagerieBridgeController messagerieController;

        public JavaBridge(WebView webView) {
            this.hostWebView = webView;
            ProjetWebContext context = new ProjetWebContext(webView, MyBD.getInstance().getConn());
            this.sessionController = new SessionBridgeController();
            this.projetController = new ProjetBridgeController(context);
            this.annulationController = new AnnulationBridgeController(context);
            this.profilController = new ProfilBridgeController(context);
            this.financementController = new FinancementBridgeController(context);
            this.messagerieController = new MessagerieBridgeController();
        }

        public String getCurrentUserName() {
            return sessionController.getCurrentUserName();
        }

        public String getCurrentUserId() {
            return sessionController.getCurrentUserId();
        }

        public String getCurrentUserRole() {
            return sessionController.getCurrentUserRole();
        }

        public String getEntrepreneurInvestmentNotificationsJson() {
            return sessionController.getEntrepreneurInvestmentNotificationsJson();
        }

        public String markEntrepreneurNotificationRead(String id) {
            return sessionController.markEntrepreneurNotificationRead(id);
        }

        public String getEntrepreneurNotificationHistoryJson(String limit) {
            return sessionController.getEntrepreneurNotificationHistoryJson(limit);
        }

        public String getInvestorProjectNotificationsJson() {
            return sessionController.getInvestorProjectNotificationsJson();
        }

        public String markInvestorNotificationRead(String id) {
            return sessionController.markInvestorNotificationRead(id);
        }

        public String getInvestorNotificationHistoryJson(String limit) {
            return sessionController.getInvestorNotificationHistoryJson(limit);
        }

        public String getCurrentUserWithProfil() {
            return profilController.getCurrentUserWithProfil();
        }

        public String getCurrentEntrepreneurId() {
            return profilController.getCurrentEntrepreneurId();
        }

        public String updateCurrentUserAndProfil(String nom,
                                                 String prenom,
                                                 String telephone,
                                                 String cin,
                                                 String dateNaissance,
                                                 String nationalite,
                                                 String adresseUser,
                                                 String ville,
                                                 String adresseProfil,
                                                 String rib,
                                                 String bio,
                                                 String photoUrl,
                                                 String secteursCsv,
                                                 String autreSecteur) {
            return profilController.updateCurrentUserAndProfil(
                    nom, prenom, telephone, cin, dateNaissance, nationalite,
                    adresseUser, ville, adresseProfil, rib, bio, photoUrl, secteursCsv, autreSecteur
            );
        }

        public String submitProfileForVerification(String adresse,
                                                   String cinRectoUrl,
                                                   String cinVersoUrl,
                                                   String justificatifDomicileUrl,
                                                   String rib,
                                                   String bio,
                                                   String registreCommerceUrl,
                                                   String patenteUrl,
                                                   String matriculeFiscalUrl,
                                                   String carteFiscaleUrl,
                                                   String secteursCsv,
                                                   String autreSecteur) {
            return profilController.submitProfileForVerification(
                    adresse, cinRectoUrl, cinVersoUrl, justificatifDomicileUrl, rib, bio,
                    registreCommerceUrl, patenteUrl, matriculeFiscalUrl, carteFiscaleUrl, secteursCsv, autreSecteur
            );
        }

        public String uploadPdf(String docType) {
            return profilController.uploadPdf(docType);
        }

        public String uploadImage(String docType) {
            return profilController.uploadImage(docType);
        }

        public String openFile(String input) {
            return profilController.openFile(input);
        }

        public String readImageAsDataUrl(String input) {
            return profilController.readImageAsDataUrl(input);
        }

        public String normalizeLocalFileUri(String input) {
            return profilController.normalizeLocalFileUri(input);
        }

        public String logout() {
            return sessionController.logout();
        }

        // --- Investisseur navigation helpers (for navbar on financement/remboursement pages) ---
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

        public String goEvenements() {
            return sessionController.goEvenements();
        }

        public String goInvitations() {
            return sessionController.goInvitations();
        }

        public String openProfilInvestisseur() {
            try {
                javafx.application.Platform.runLater(() ->
                        sceneManager.switchTo("/profil_investisseur_view.fxml", "Investia - Mon Profil (Investisseur)")
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

        public String listProjets() {
            return projetController.listProjets();
        }

        public String listMyProjets() {
            return projetController.listMyProjets();
        }

        public String getProjetById(String id) {
            return projetController.getProjetById(id);
        }

        public String addProjet(String entrepreneurId,
                                String statut,
                                String titre,
                                String secteur,
                                String descriptionCourte,
                                String descriptionLongue,
                                String objectifTnd,
                                String dureeCampagneJours,
                                String modeRemboursement,
                                String tauxInteretPct,
                                String dureeRemboursementMois,
                                String margeBruteEstimeeTnd,
                                String resultatNetEstimeTnd) {
            return projetController.addProjet(
                    entrepreneurId, statut, titre, secteur, descriptionCourte, descriptionLongue,
                    objectifTnd, dureeCampagneJours, modeRemboursement, tauxInteretPct,
                    dureeRemboursementMois, margeBruteEstimeeTnd, resultatNetEstimeTnd
            );
        }

        public String updateProjet(String idProjet,
                                   String entrepreneurId,
                                   String statut,
                                   String titre,
                                   String secteur,
                                   String descriptionCourte,
                                   String descriptionLongue,
                                   String objectifTnd,
                                   String dureeCampagneJours,
                                   String modeRemboursement,
                                   String tauxInteretPct,
                                   String dureeRemboursementMois,
                                   String margeBruteEstimeeTnd,
                                   String resultatNetEstimeTnd) {
            return projetController.updateProjet(
                    idProjet, entrepreneurId, statut, titre, secteur, descriptionCourte, descriptionLongue,
                    objectifTnd, dureeCampagneJours, modeRemboursement, tauxInteretPct,
                    dureeRemboursementMois, margeBruteEstimeeTnd, resultatNetEstimeTnd
            );
        }

        public String publishProjet(String idProjet) {
            return projetController.publishProjet(idProjet);
        }

        public String getInvestissementsByProjetJson(String idProjet) {
            return projetController.getInvestissementsByProjetJson(idProjet);
        }

        public String getInvestisseursContactParProjetJson() {
            return projetController.getInvestisseursContactParProjetJson();
        }

        public String exportCurrentPagePdfWithPdflayer(String documentUrl,
                                                       String documentHtml,
                                                       String pageSize,
                                                       String marginTop,
                                                       String marginBottom,
                                                       String marginLeft,
                                                       String marginRight) {
            return projetController.exportCurrentPagePdfWithPdflayer(
                    documentUrl, documentHtml, pageSize, marginTop, marginBottom, marginLeft, marginRight
            );
        }

        public String addSuiviTache(String idProjet,
                                    String titre,
                                    String description,
                                    String dateDebut,
                                    String dateFin,
                                    String progressionDelta,
                                    String coutTache) {
            return projetController.addSuiviTache(idProjet, titre, description, dateDebut, dateFin, progressionDelta, coutTache);
        }

        public String updateSuiviTache(String tacheId,
                                       String titre,
                                       String description,
                                       String dateDebut,
                                       String dateFin,
                                       String progressionDelta,
                                       String coutTache) {
            return projetController.updateSuiviTache(tacheId, titre, description, dateDebut, dateFin, progressionDelta, coutTache);
        }

        public String deleteSuiviTache(String tacheId) {
            return projetController.deleteSuiviTache(tacheId);
        }

        public String getSuiviProjetJson(String idProjet) {
            return projetController.getSuiviProjetJson(idProjet);
        }

        public String addSuiviCharge(String idProjet, String description, String montant, String dateFlux) {
            return projetController.addSuiviCharge(idProjet, description, montant, dateFlux);
        }

        public String addSuiviGain(String idProjet, String description, String montant, String dateFlux) {
            return projetController.addSuiviGain(idProjet, description, montant, dateFlux);
        }

        public String updateSuiviCharge(String fluxId, String description, String montant, String dateFlux) {
            return projetController.updateSuiviCharge(fluxId, description, montant, dateFlux);
        }

        public String updateSuiviGain(String fluxId, String description, String montant, String dateFlux) {
            return projetController.updateSuiviGain(fluxId, description, montant, dateFlux);
        }

        public String deleteSuiviCharge(String fluxId) {
            return projetController.deleteSuiviCharge(fluxId);
        }

        public String deleteSuiviGain(String fluxId) {
            return projetController.deleteSuiviGain(fluxId);
        }

        public String syncTaskToCalendar(String tacheId) {
            return projetController.syncTaskToCalendar(tacheId);
        }

        public String syncProjectTasksToCalendar(String idProjet) {
            return projetController.syncProjectTasksToCalendar(idProjet);
        }

        public String getCalendarConfigStatus() {
            return projetController.getCalendarConfigStatus();
        }

        public String getCalendarEmbedUrl() {
            return projetController.getCalendarEmbedUrl();
        }

        public String openGoogleCalendarInApp() {
            try {
                javafx.application.Platform.runLater(() -> {
                    Stage stage = new Stage();
                    stage.setTitle("Investia - Google Calendar");
                    stage.initModality(Modality.NONE);
                    if (hostWebView != null && hostWebView.getScene() != null && hostWebView.getScene().getWindow() != null) {
                        stage.initOwner(hostWebView.getScene().getWindow());
                    }

                    WebView view = new WebView();
                    WebEngine engine = view.getEngine();
                    engine.setJavaScriptEnabled(true);
                    engine.setCreatePopupHandler(config -> engine);
                    engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            try {
                                Object winObj = engine.executeScript("window");
                                if (winObj instanceof JSObject window) {
                                    window.setMember("javaBridge", this);
                                    engine.executeScript("window.__bridgeReady = true;");
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });

                    engine.load("https://calendar.google.com/calendar/u/0/r");

                    Scene scene = new Scene(view, 1280, 820);
                    stage.setScene(scene);
                    stage.show();
                    calendarStage = stage;
                    calendarEngine = engine;
                });
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String closeGoogleCalendarInApp() {
            try {
                javafx.application.Platform.runLater(() -> {
                    if (calendarStage != null) {
                        calendarStage.close();
                        calendarStage = null;
                    }
                    calendarEngine = null;
                });
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        public String refreshGoogleCalendarInApp() {
            try {
                javafx.application.Platform.runLater(() -> {
                    if (calendarEngine != null) {
                        try {
                            calendarEngine.reload();
                        } catch (Exception ignored) {
                        }
                    }
                });
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }

        private void navigateHostTo(String page) {
            try {
                if (hostWebView == null || hostWebView.getEngine() == null || page == null || page.isBlank()) return;
                hostWebView.getEngine().executeScript("window.location.href='" + page.replace("'", "") + "'");
            } catch (Exception ignored) {
            }
        }

        public String addDemandeAnnulation(String projetId, String raison) {
            return annulationController.addDemandeAnnulation(projetId, raison);
        }

        public String listDemandesAnnulation() {
            return annulationController.listDemandesAnnulation();
        }

        public String listMyDemandesAnnulation() {
            return annulationController.listMyDemandesAnnulation();
        }

        public String getProfilById(String idEntrepreneur) {
            return profilController.getProfilById(idEntrepreneur);
        }

        public String getProfilByUserId(String idUser) {
            return profilController.getProfilByUserId(idUser);
        }

        public String addProfil(String idUser,
                                String adresse,
                                String cinRectoUrl,
                                String cinVersoUrl,
                                String justificatifDomicileUrl,
                                String rib,
                                String accepteConditions,
                                String statutCompte,
                                String statutVerification,
                                String dateVerification,
                                String bio,
                                String photoUrl,
                                String registreCommerceUrl,
                                String patenteUrl,
                                String matriculeFiscalUrl,
                                String carteFiscaleUrl,
                                String secteursCsv) {
            return profilController.addProfil(
                    idUser, adresse, cinRectoUrl, cinVersoUrl, justificatifDomicileUrl, rib,
                    accepteConditions, statutCompte, statutVerification, dateVerification, bio,
                    photoUrl, registreCommerceUrl, patenteUrl, matriculeFiscalUrl, carteFiscaleUrl, secteursCsv
            );
        }

        public String updateProfil(String idEntrepreneur,
                                   String idUser,
                                   String adresse,
                                   String cinRectoUrl,
                                   String cinVersoUrl,
                                   String justificatifDomicileUrl,
                                   String rib,
                                   String accepteConditions,
                                   String statutCompte,
                                   String statutVerification,
                                   String dateVerification,
                                   String bio,
                                   String photoUrl,
                                   String registreCommerceUrl,
                                   String patenteUrl,
                                   String matriculeFiscalUrl,
                                   String carteFiscaleUrl,
                                   String secteursCsv) {
            return profilController.updateProfil(
                    idEntrepreneur, idUser, adresse, cinRectoUrl, cinVersoUrl, justificatifDomicileUrl, rib,
                    accepteConditions, statutCompte, statutVerification, dateVerification, bio, photoUrl,
                    registreCommerceUrl, patenteUrl, matriculeFiscalUrl, carteFiscaleUrl, secteursCsv
            );
        }

        // --- Financement / Remboursement (Web UI) ---
        public String getProjectsJson() {
            return financementController.getProjectsJson();
        }

        public String getMyProjectsJson() {
            return financementController.getMyProjectsJson();
        }

        public String getProjectDashboardJson(int projectId) {
            return financementController.getProjectDashboardJson(projectId);
        }

        public String getInvestissementsJson() {
            return financementController.getInvestissementsJson();
        }

        public String getFinancementsByProjectJson(int projectId) {
            return financementController.getFinancementsByProjectJson(projectId);
        }

        public String getFinancementsByProjectWithRembStatusJson(int projectId) {
            return financementController.getFinancementsByProjectWithRembStatusJson(projectId);
        }

        public String getFinancementsJson() {
            return financementController.getFinancementsJson();
        }

        public String getFinancementsForCurrentInvestorJson() {
            return financementController.getFinancementsForCurrentInvestorJson();
        }

        public String getFinancementByIdJson(int id) {
            return financementController.getFinancementByIdJson(id);
        }

        public String createFinancementFromJs(String payload) {
            return financementController.createFinancementFromJs(payload);
        }

        public String updateFinancementFromJs(String payload) {
            return financementController.updateFinancementFromJs(payload);
        }

        public String deleteFinancementFromJs(String payload) {
            return financementController.deleteFinancementFromJs(payload);
        }

        public String deleteFinancement(int id) {
            return financementController.deleteFinancement(id);
        }

        public String getRemboursementsJson() {
            return financementController.getRemboursementsJson();
        }

        public String getRemboursementsByProjectJson(int projectId) {
            return financementController.getRemboursementsByProjectJson(projectId);
        }

        public String getRemboursementsForCurrentInvestorJson() {
            return financementController.getRemboursementsForCurrentInvestorJson();
        }

        public String getRemboursementsByFinancementJson(int financementId) {
            return financementController.getRemboursementsByFinancementJson(financementId);
        }

        public String getRemboursementByIdJson(int id) {
            return financementController.getRemboursementByIdJson(id);
        }

        public String createRemboursementFromJs(String payload) {
            return financementController.createRemboursementFromJs(payload);
        }

        public String updateRemboursementFromJs(String payload) {
            return financementController.updateRemboursementFromJs(payload);
        }

        public String deleteRemboursementFromJs(String payload) {
            return financementController.deleteRemboursementFromJs(payload);
        }

        public String payRemboursementFromJs(String payload) {
            return financementController.payRemboursementFromJs(payload);
        }

        public String askChatbot(String userMessage) {
            String apiKey = resolveGroqApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return "ERROR: Missing GROQ_API_KEY (env var, JVM property, or .env file).";
            }
            try {
                GroqChatService chatService = new GroqChatService(apiKey, "openai/gpt-oss-120b");
                String cleanMessage = userMessage == null ? "" : userMessage.trim();
                if (cleanMessage.isEmpty()) {
                    return "Pose-moi une question et je te reponds.";
                }
                User currentUser = Session.getCurrentUser();
                ChatbotProjectContextService contextService = new ChatbotProjectContextService();
                String quickAnswer = contextService.tryQuickAnswer(currentUser, cleanMessage);
                if (quickAnswer != null && !quickAnswer.isBlank()) return quickAnswer;
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

        public String getSelectedContactUserId() {
            return messagerieController.getSelectedContactUserId();
        }

        public String setSelectedContactUserId(String id) {
            return messagerieController.setSelectedContactUserId(id);
        }

        public String getMessagerieContactsJson() {
            return messagerieController.getMessagerieContactsJson();
        }

        public String getConversationMessagesJson(String otherUserId) {
            return messagerieController.getConversationMessagesJson(otherUserId);
        }

        public String sendMessageToUser(String otherUserId, String contenu) {
            return messagerieController.sendMessageToUser(otherUserId, contenu);
        }

        public String getUserSummaryJson(String userId) {
            return messagerieController.getUserSummaryJson(userId);
        }

        public String getUnreadMessagesCount() {
            return messagerieController.getUnreadMessagesCount();
        }

        public String openExternalUrl(String url) {
            try {
                String target = url == null ? "" : url.trim();
                if (target.isEmpty()) return "ERROR:URL_REQUIRED";
                if (!Desktop.isDesktopSupported()) return "ERROR:DESKTOP_NOT_SUPPORTED";
                Desktop desktop = Desktop.getDesktop();
                if (!desktop.isSupported(Desktop.Action.BROWSE)) return "ERROR:BROWSE_NOT_SUPPORTED";
                desktop.browse(new URI(target));
                return "OK";
            } catch (Exception e) {
                return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
            }
        }
    }
}

