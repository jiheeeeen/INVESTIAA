package org.example;

import Utils.sceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainFX extends Application {
    static {
        configureSafeGraphicsPipeline();
    }

    private static void configureSafeGraphicsPipeline() {
        // Must be set before JavaFX toolkit initialization.
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.d3d", "false");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("prism.vsync", "false");
    }

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println(
                "JavaFX pipeline: prism.order=" + System.getProperty("prism.order")
                        + ", prism.d3d=" + System.getProperty("prism.d3d")
                        + ", sun.java2d.d3d=" + System.getProperty("sun.java2d.d3d")
        );
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
        configureSafeGraphicsPipeline();
        launch(args);
    }
}
