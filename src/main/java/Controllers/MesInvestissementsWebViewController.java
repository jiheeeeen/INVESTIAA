package Controllers;

import Entities.User;
import Utils.Session;
import Utils.WebViewBridgeUtil;
import Utils.sceneManager;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;

public class MesInvestissementsWebViewController {

    @FXML
    private WebView webView;

    private final JavaBridgeMesInvestissements investBridge = new JavaBridgeMesInvestissements();

    @FXML
    private void initialize() {

        System.out.println("Ã¢Å“â€¦ OPEN: MesInvestissementsWebViewController.initialize()");

        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        URL url = getClass().getResource("/web/mes_investissements_view.html");
        System.out.println("Ã¢Å“â€¦ HTML URL = " + (url == null ? "NULL" : url.toExternalForm()));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                System.out.println("Ã¢Å“â€¦ WebView load SUCCEEDED (mes_investissements_view.html)");

                // Ã¢Å“â€¦ Toujours injecter sur le JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Ã¢Å“â€¦ COMME AVANT : javaBridge + alias + investBridge
                        WebViewBridgeUtil.injectAll(engine, this, investBridge);

                        // Ã¢Å“â€¦ Un seul appel suffit (le navbar.js fait dÃƒÂ©jÃƒÂ  des retries si besoin)
                        WebViewBridgeUtil.safeExec(engine,
                                "if(window.loadMesInvestissements) window.loadMesInvestissements();"
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        if (url != null) engine.load(url.toExternalForm());
        else engine.loadContent("<h2>mes_investissements_view.html introuvable</h2>");
    }

    // ==========================
    // Navbar helpers
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
        try {
            User u = Session.getCurrentUser();
            if (u == null) return "";
            String nom = u.getNom() == null ? "" : u.getNom().trim();
            String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
            String full = (nom + " " + prenom).trim();
            return full.isEmpty() ? (u.getEmail() == null ? "" : u.getEmail().trim()) : full;
        } catch (Exception e) {
            return "";
        }
    }

    // ==========================
    // Navigation (appelÃƒÂ©e par navbar.js)
    // ==========================
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

    public String goAccueilInvestisseur() {
        try {
            javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorAccueil);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String openProfilInvestisseur() {
        try {
            javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProfil);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String openEditProfilInvestisseur() {
        try {
            javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorEditProfil);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String openProjetsInvestisseur() {
        try {
            javafx.application.Platform.runLater(InvestisseurWebViewController::openInvestorProjets);
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    public String openWalletInvestisseur() {
        try {
            javafx.application.Platform.runLater(() ->
                    sceneManager.switchTo("/web/wallet_investisseur_view.fxml", "Investia - Mon Wallet")
            );
            return "OK";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    // Ã¢Å“â€¦ AJOUT MINIMAL : alias (si navbar appelle openWallet)
    public String openWallet() {
        return openWalletInvestisseur();
    }
}

