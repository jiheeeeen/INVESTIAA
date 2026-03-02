package Controllers;

import Services.EvenementService;
import Entities.Evenement;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bridge Java ↔ JS pour la fenêtre modifierEvenement.html.
 * Exposé sous window.modifierBridge.
 */
public class ModifierBridge {

    private final WebEngine      engine;
    private final Stage          stage;
    private final Runnable       onSuccess;
    private final EvenementService service = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ✅ Référence forte — empêche le GC
    private JSObject jsWindow;

    public ModifierBridge(WebEngine engine, Stage stage, Runnable onSuccess) {
        this.engine    = engine;
        this.stage     = stage;
        this.onSuccess = onSuccess;
    }

    // ── MODIFIER ──
    public String modifier(int id, String titre, int projectId, String description,
                           String mode, String dateDebut, String dateFin,
                           String lieu, String meetingLink, int organisateurId) {
        try {
            Evenement e = new Evenement(
                    id, projectId, titre, description, mode,
                    LocalDateTime.parse(dateDebut, FMT),
                    LocalDateTime.parse(dateFin,   FMT),
                    lieu, meetingLink, organisateurId
            );
            service.modifier(e);

            // Notifier la page principale de recharger la liste
            if (onSuccess != null) {
                Platform.runLater(onSuccess);
            }
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR:" + (e.getMessage() == null ? "UNKNOWN" : e.getMessage());
        }
    }

    // ── FERMER LA FENÊTRE ──
    public void closeModifierWindow() {
        Platform.runLater(() -> {
            if (stage != null) stage.close();
        });
    }
}