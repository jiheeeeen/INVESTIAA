package Controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class AdminDashboardController {

    @FXML private WebView webView;
    private WebEngine engine;

    // ✅ 1 seule instance (évite les surprises)
    private final AdminDashboardBridge bridge = new AdminDashboardBridge();

    @FXML
    public void initialize() {
        engine = webView.getEngine();

        // Debug alert JS
        engine.setOnAlert(e -> System.out.println("JS alert => " + e.getData()));

        engine.setConfirmHandler(message -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Confirmation");
            a.setHeaderText(null);
            a.setContentText(message);
            return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
        });

        engine.setPromptHandler(param -> {
            TextInputDialog d = new TextInputDialog(param.getDefaultValue());
            d.setTitle("Saisie");
            d.setHeaderText(null);
            d.setContentText(param.getMessage());
            return d.showAndWait().orElse(null);
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");

                // ✅ Injection
                window.setMember("admin", bridge);

                // ✅ Vérif methods (ça te dira DIRECT si c'est undefined)
                engine.executeScript(
                        "alert('✅ Admin bridge OK: ' + (window.admin ? 'YES' : 'NO') + " +
                                "' | acceptAccount=' + typeof window.admin.acceptAccount + " +
                                "' | rejectAccount=' + typeof window.admin.rejectAccount + " +
                                "' | deleteUser=' + typeof window.admin.deleteUser + " +
                                "' | updateUser=' + typeof window.admin.updateUser);"
                );

                // ✅ (Optionnel) appelle bootstrap après injection
                engine.executeScript("if(window.bootstrapAdmin) window.bootstrapAdmin();");
            }
        });

        loadDashboard();
    }

    private void loadDashboard() {
        String url = getClass().getResource("/html/admin_dashboard.html").toExternalForm();
        engine.load(url + "?v=" + System.currentTimeMillis()); // anti-cache
    }
}
