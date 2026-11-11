package com.example.tienlen.client.ui;

import java.util.Random;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Màn hình vào bàn gọn, tập trung vào tên, số người và mã bàn. */
public class LobbyView extends StackPane {
    private final TextField nameField = new TextField();
    private final ComboBox<String> modeCombo = new ComboBox<>();
    private final TextField roomIdField = new TextField();
    private final Button randomCodeButton = new Button("Đổi mã");
    private final Button createRoomButton = new Button("Tạo bàn");
    private final Button joinRoomButton = new Button("Vào bàn");
    private final Button quickJoinButton = new Button("Chơi nhanh");
    private final Label statusLabel = new Label();
    private final Random random = new Random();

    public LobbyView() {
        getStyleClass().add("game-root");
        setPadding(new Insets(28));

        Label title = new Label("Tiến Lên Online");
        title.getStyleClass().add("lobby-title");
        Label subtitle = new Label("Vào bàn cùng bạn bè");
        subtitle.getStyleClass().add("lobby-subtitle");

        nameField.setText("Người chơi " + (100 + random.nextInt(900)));
        nameField.setPromptText("Tên người chơi");

        modeCombo.getItems().addAll("Bàn 4 người", "Bàn 3 người", "Bàn 2 người");
        modeCombo.setValue("Bàn 4 người");
        modeCombo.setMaxWidth(Double.MAX_VALUE);

        if (com.example.tienlen.client.ClientInstanceDetector.getInstanceNumber() > 1) {
            roomIdField.setText(com.example.tienlen.client.ClientInstanceDetector.getLastRoomCode());
        } else {
            generateRandomRoomCode();
        }
        roomIdField.setPromptText("Mã bàn 6 số");

        randomCodeButton.getStyleClass().add("field-button");
        randomCodeButton.setOnAction(event -> generateRandomRoomCode());
        HBox roomField = new HBox(7, roomIdField, randomCodeButton);
        HBox.setHgrow(roomIdField, Priority.ALWAYS);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);
        addFormRow(form, 0, "Tên", nameField);
        addFormRow(form, 1, "Số người", modeCombo);
        addFormRow(form, 2, "Mã bàn", roomField);

        createRoomButton.getStyleClass().addAll("game-button", "primary-button");
        joinRoomButton.getStyleClass().addAll("game-button", "secondary-button");
        quickJoinButton.getStyleClass().add("link-button");
        createRoomButton.setMaxWidth(Double.MAX_VALUE);
        joinRoomButton.setMaxWidth(Double.MAX_VALUE);

        HBox mainActions = new HBox(8, createRoomButton, joinRoomButton);
        mainActions.setAlignment(Pos.CENTER);
        HBox.setHgrow(createRoomButton, Priority.ALWAYS);
        HBox.setHgrow(joinRoomButton, Priority.ALWAYS);

        statusLabel.getStyleClass().add("lobby-status");
        statusLabel.setWrapText(true);
        statusLabel.setText("Sẵn sàng vào bàn");

        VBox panel = new VBox(16, title, subtitle, form, mainActions, quickJoinButton, statusLabel);
        panel.getStyleClass().add("lobby-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(430);
        panel.setPadding(new Insets(26));

        getChildren().add(panel);
    }

    private void addFormRow(GridPane grid, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        if (field instanceof javafx.scene.layout.Region region) {
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
    }

    public void generateRandomRoomCode() {
        String code = String.valueOf(100000 + random.nextInt(900000));
        roomIdField.setText(code);
        com.example.tienlen.client.ClientInstanceDetector.saveLastRoomCode(code);
    }

    public String getPlayerName() {
        return nameField.getText().trim();
    }

    public void setPlayerName(String name) {
        nameField.setText(name);
    }

    public int getPlayerCount() {
        String selected = modeCombo.getValue();
        if (selected == null) {
            return 4;
        }
        if (selected.contains("2 người")) {
            return 2;
        }
        if (selected.contains("3 người")) {
            return 3;
        }
        return 4;
    }

    public String getRoomId() {
        return roomIdField.getText().trim();
    }

    /**
     * Địa chỉ server được giữ ngoài giao diện để màn hình vào bàn luôn gọn.
     * Có thể thay bằng tham số JVM -Dtienlen.server.host=x.x.x.x.
     */
    public String getServerHost() {
        return System.getProperty(
                "tienlen.server.host",
                com.example.tienlen.client.ClientInstanceDetector.getLastServerHost()
        ).trim();
    }

    public void setRoomId(String roomId) {
        roomIdField.setText(roomId);
    }

    public Button getCreateRoomButton() {
        return createRoomButton;
    }

    public Button getJoinRoomButton() {
        return joinRoomButton;
    }

    public Button getQuickJoinButton() {
        return quickJoinButton;
    }

    public void setStatus(String message) {
        statusLabel.getStyleClass().remove("status-error");
        statusLabel.setText(message);
    }

    public void setErrorStatus(String message) {
        if (!statusLabel.getStyleClass().contains("status-error")) {
            statusLabel.getStyleClass().add("status-error");
        }
        statusLabel.setText(message);
    }

    public void setBusy(boolean busy) {
        createRoomButton.setDisable(busy);
        joinRoomButton.setDisable(busy);
        quickJoinButton.setDisable(busy);
        nameField.setDisable(busy);
        modeCombo.setDisable(busy);
        roomIdField.setDisable(busy);
        randomCodeButton.setDisable(busy);
    }
}
