package org.example;

import Controllers.AnnulationBridgeController;
import Controllers.FinancementBridgeController;
import Controllers.ProfilBridgeController;
import Controllers.ProjetBridgeController;
import Controllers.ProjetWebContext;
import Controllers.SessionBridgeController;
import Entities.Role;
import Entities.StatutVerification;
import Entities.User;
import Utils.Session;
import Utils.sceneManager;
import java.net.URL;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class ProjetWebViewController {
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
                return getClass().getResource("/accueil_investisseur.html");
            }
            return getClass().getResource("/accueil.html");
        }

        if (current.getStatutVerification() == StatutVerification.NON_VERIFIE
                && current.getRole() == Role.ENTREPRENEUR) {
            return getClass().getResource("/completerInfos.html");
        }

        return getClass().getResource("/attenteValidation.html");
    }

    public class JavaBridge {
        private final SessionBridgeController sessionController;
        private final ProjetBridgeController projetController;
        private final AnnulationBridgeController annulationController;
        private final ProfilBridgeController profilController;
        private final FinancementBridgeController financementController;

        public JavaBridge(WebView webView) {
            ProjetWebContext context = new ProjetWebContext(webView);
            this.sessionController = new SessionBridgeController();
            this.projetController = new ProjetBridgeController(context);
            this.annulationController = new AnnulationBridgeController(context);
            this.profilController = new ProfilBridgeController(context);
            this.financementController = new FinancementBridgeController(context);
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
    }
}
