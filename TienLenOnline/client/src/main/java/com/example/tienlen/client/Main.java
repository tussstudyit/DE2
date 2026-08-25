package com.example.tienlen.client;

import com.example.tienlen.client.controller.GameController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        GameController controller = new GameController();
        Scene scene = new Scene(controller.getView(), 1180, 760);

        var stylesheet = Main.class.getResource("/com/example/tienlen/client/styles/game.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Tiến Lên Online");
        stage.setMinWidth(960);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
