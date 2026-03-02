package Controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controller JavaFX pour invitation.html
 * Pattern identique à AjoutEvenementController
 */
public class InvitationController {

    @FXML
    private WebView webView;

    private WebEngine engine;

    // ✅ Champ de classe — jamais variable locale
    private InvitationBridge bridge;

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        engine.setOnAlert(e -> System.out.println("[InvitationJS] " + e.getData()));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    bridge = new InvitationBridge(engine);
                    bridge.inject();

                    // Charger tous les événements + invitations
                    String json = bridge.getAllData();
                    engine.executeScript("renderAll(" + json + ")");

                    System.out.println("✅ InvitationBridge injecté + données chargées");
                } catch (Exception e) {
                    System.err.println("❌ Erreur InvitationController : " + e.getMessage());
                    e.printStackTrace();
                }
            }
            if (newState == Worker.State.FAILED) {
                System.err.println("❌ Échec chargement invitation.html");
            }
        });

        loadPage();
    }

    private void loadPage() {
        java.net.URL url = getClass().getResource("/web/invitation.html");
        if (url == null) {
            System.err.println("❌ Fichier introuvable : /web/invitation.html");
            return;
        }
        engine.load(url.toExternalForm() + "?v=" + System.currentTimeMillis());
    }
}
