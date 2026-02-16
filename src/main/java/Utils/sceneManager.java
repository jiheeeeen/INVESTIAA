package Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.Objects;

public class sceneManager {

    private static Stage primaryStage;

    public static void init(Stage stage) {
        primaryStage = stage;

        // Taille "web app"
        primaryStage.setWidth(1200);
        primaryStage.setHeight(750);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);

        // Icône fenêtre
        try {
            Image icon = new Image(Objects.requireNonNull(
                    sceneManager.class.getResourceAsStream("/img/logo.png")
            ));
            primaryStage.getIcons().add(icon);
        } catch (Exception ignored) {}
    }

    public static void switchTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(sceneManager.class.getResource(fxmlPath)));
            Parent newRoot = loader.load();

            // Appliquer CSS global
            Scene currentScene = primaryStage.getScene();
            if (currentScene == null) {
                Scene scene = new Scene(newRoot, 1200, 720); // taille “web”
                scene.getStylesheets().add(Objects.requireNonNull(sceneManager.class.getResource("/css/app.css")).toExternalForm());
                primaryStage.setTitle(title);
                primaryStage.setScene(scene);
                primaryStage.show();
                return;
            }

            // Transition: fade-out l'ancien root
            Parent oldRoot = currentScene.getRoot();

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), oldRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(ev -> {
                currentScene.setRoot(newRoot);

                // fade-in nouveau root
                newRoot.setOpacity(0.0);
                javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), newRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            primaryStage.setTitle(title);
            fadeOut.play();

        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger: " + fxmlPath, e);
        }
    }

}
