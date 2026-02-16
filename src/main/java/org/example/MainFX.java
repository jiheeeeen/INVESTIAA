package org.example;

import Utils.sceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainFX extends Application {
    static {
        // Apply before JavaFX toolkit initialization.
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.d3d", "false");
    }

    @Override
    public void start(Stage stage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception in thread " + (thread == null ? "unknown" : thread.getName()));
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });
        sceneManager.init(stage);
        sceneManager.switchTo("/web_auth.fxml", "Investia - Inscription");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
