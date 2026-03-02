package Controllers;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

/**
 * Ouvre une nouvelle fenêtre JavaFX avec modifierEvenement.html.
 * Appelé depuis EvenementBridge.openModifierWindow().
 */
public class Modifierevenementcontroller {

    private Stage           stage;
    private WebEngine       engine;
    private ModifierBridge  bridge;

    // Données de l'événement à pré-remplir
    private final int    id;
    private final String titre, description, mode, dateDebut, dateFin, lieu, meetingLink;
    private final int    projectId, organisateurId;

    // Callback appelé après une modification réussie → recharge la liste principale
    private final Runnable onSuccess;

    public Modifierevenementcontroller(int id, String titre, int projectId, int organisateurId,
                                       String description, String mode, String dateDebut, String dateFin,
                                       String lieu, String meetingLink, Runnable onSuccess) {
        this.id             = id;
        this.titre          = titre;
        this.projectId      = projectId;
        this.organisateurId = organisateurId;
        this.description    = description;
        this.mode           = mode;
        this.dateDebut      = dateDebut;
        this.dateFin        = dateFin;
        this.lieu           = lieu;
        this.meetingLink    = meetingLink;
        this.onSuccess      = onSuccess;
    }

    public void show() {
        Platform.runLater(() -> {
            WebView webView = new WebView();
            engine = webView.getEngine();

            // Debug JS
            engine.setOnAlert(e -> System.out.println("[ModifierJS] " + e.getData()));

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        // 1. Créer et injecter le bridge sous window.modifierBridge
                        bridge = new ModifierBridge(engine, stage, onSuccess);
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("modifierBridge", bridge);

                        // 2. Préremplir le formulaire via prefill()
                        engine.executeScript(buildPrefillScript());

                        System.out.println("✅ ModifierBridge injecté + formulaire prérempli");
                    } catch (Exception e) {
                        System.err.println("❌ Erreur ModifierEvenementController : " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

            // Charger modifierEvenement.html
            java.net.URL url = getClass().getResource("/html/modifierEvenement.html");
            if (url == null) {
                System.err.println("❌ Fichier introuvable : /html/modifierEvenement.html");
                return;
            }
            engine.load(url.toExternalForm() + "?v=" + System.currentTimeMillis());

            // Créer la fenêtre
            stage = new Stage();
            stage.setTitle("Modifier l'événement — #" + id);
            stage.setScene(new Scene(webView, 720, 680));
            stage.initModality(Modality.APPLICATION_MODAL); // bloque la fenêtre principale
            stage.setResizable(true);
            stage.show();
        });
    }

    /**
     * Construit l'appel JS prefill(...) avec les données de l'événement.
     * Utilise JSON.stringify pour éviter tout problème de quotes.
     */
    private String buildPrefillScript() {
        return "prefill("
                + id + ","
                + jsStr(titre)          + ","
                + projectId             + ","
                + organisateurId        + ","
                + jsStr(description)    + ","
                + jsStr(mode)           + ","
                + jsStr(dateDebut)      + ","
                + jsStr(dateFin)        + ","
                + jsStr(lieu)           + ","
                + jsStr(meetingLink)
                + ");";
    }

    /** Échappe une String pour l'injecter dans du JS entre apostrophes */
    private String jsStr(String s) {
        if (s == null) return "''";
        return "'" + s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n") + "'";
    }
}