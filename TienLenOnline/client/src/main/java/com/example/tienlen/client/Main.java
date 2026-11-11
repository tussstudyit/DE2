package com.example.tienlen.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static String initialPlayerName = null;

    public static void setInitialPlayerName(String name) {
        initialPlayerName = name;
    }

    @Override
    public void start(Stage stage) {
        GameController controller = new GameController();
        if (initialPlayerName != null && !initialPlayerName.isBlank()) {
            controller.getLobbyView().setPlayerName(initialPlayerName);
        }

        Scene scene = new Scene(controller.getRoot(), 1180, 780);

        var stylesheet = Main.class.getResource("/styles/game.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        String titleSuffix = (initialPlayerName != null && !initialPlayerName.isBlank())
                ? " [" + initialPlayerName + "]"
                : "";
        stage.setTitle("Tiến Lên Miền Nam Online - Java RMI" + titleSuffix);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        if (args.length > 0 && (initialPlayerName == null || initialPlayerName.isBlank())) {
            initialPlayerName = args[0];
        }
        launch(args);
    }
}
