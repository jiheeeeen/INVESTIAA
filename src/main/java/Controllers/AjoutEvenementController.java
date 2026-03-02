package Controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Controller JavaFX pour ajoutEvenement.html.
 * Pattern identique à AdminDashboardController.java.
 */
public class AjoutEvenementController {

    @FXML
    private WebView webView;

    private WebEngine engine;

    // ✅ Champ de classe — JAMAIS variable locale (sinon GC détruit le bridge)
    private EvenementBridge bridge;

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        // Debug JS → console Java
        engine.setOnAlert(e -> System.out.println("[JS] " + e.getData()));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {

            if (newState == Worker.State.SUCCEEDED) {
                try {
                    // 1. Créer le bridge
                    bridge = new EvenementBridge(engine);

                    // 2. Injecter window.javaBridge
                    bridge.inject();

                    // 3. Charger la liste dans le HTML
                    String json = bridge.getAll();
                    engine.executeScript("renderTable(" + json + ")");

                    System.out.println("✅ Bridge injecté + liste chargée");

                } catch (Exception e) {
                    System.err.println("❌ Erreur bridge : " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (newState == Worker.State.FAILED) {
                System.err.println("❌ Échec chargement ajoutEvenement.html");
            }
        });

        loadPage();
    }

    private void loadPage() {
        java.net.URL url = getClass().getResource("/web/ajoutEvenement.html");
        if (url == null) {
            System.err.println("❌ Fichier introuvable : /web/ajoutEvenement.html");
            return;
        }
        engine.load(url.toExternalForm() + "?v=" + System.currentTimeMillis());
    }
}
