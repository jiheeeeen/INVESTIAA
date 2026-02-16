package Controllers;

import Services.UserCRUD;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class WebAuthController {
    private static volatile boolean openLoginOnNextLoad = false;

    @FXML private WebView webView;

    private WebEngine engine;
    private final UserCRUD userCRUD = new UserCRUD();
    private WebAuthBridge authBridge;

    @FXML
    public void initialize() {
        engine = webView.getEngine();
        authBridge = new WebAuthBridge(userCRUD, this::loadLogin, this::loadSignup);

        engine.setOnAlert(e -> {
            System.out.println("JS alert => " + e.getData());
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a =
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setTitle("JS Alert");
                a.setHeaderText(null);
                a.setContentText(e.getData());
                a.showAndWait();
            });
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {

                JSObject window = (JSObject) engine.executeScript("window");


                window.setMember("app", authBridge);
                window.setMember("Bridge", authBridge);

                // debug JS
                engine.executeScript("console.log('✅ app injected', window.app);");
            }
        });

        if (openLoginOnNextLoad) {
            openLoginOnNextLoad = false;
            loadLogin();
        } else {
            loadSignup();
        }
    }

    public static void openLoginOnNextLoad() {
        openLoginOnNextLoad = true;
    }

    private void loadLogin() {
        engine.load(getClass().getResource("/html/login.html").toExternalForm());
    }

    private void loadSignup() {
        engine.load(getClass().getResource("/html/creer_compte.html").toExternalForm());
    }
}
