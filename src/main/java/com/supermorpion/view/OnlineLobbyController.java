package com.supermorpion.view;

import com.supermorpion.model.Player;
import com.supermorpion.online.OnlineClient;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class OnlineLobbyController implements Initializable {

    @FXML private StackPane lobbyRoot;

    // ── Panels ────────────────────────────────────────────────────────────────
    @FXML private VBox modePanel;
    @FXML private VBox waitingPanel;
    @FXML private VBox errorPanel;

    // ── Mode panel ────────────────────────────────────────────────────────────
    @FXML private TextField serverUrlField;
    @FXML private TextField roomCodeInput;

    // ── Waiting panel ─────────────────────────────────────────────────────────
    @FXML private VBox   codeBox;
    @FXML private Label  roomCodeLabel;
    @FXML private Label  waitingStatusLabel;

    // ── Error panel ───────────────────────────────────────────────────────────
    @FXML private Label errorLabel;

    private OnlineClient client;
    private Player       assignedPlayer = Player.NONE;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serverUrlField.setText(OnlineClient.DEFAULT_SERVER);
        lobbyRoot.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lobbyRoot);
        ft.setToValue(1);
        ft.play();
    }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML
    private void createRoom() {
        connectToServer(() -> client.send("{\"type\":\"create\"}"));
    }

    @FXML
    private void joinRoom() {
        String code = roomCodeInput.getText().replaceAll("\\s+", "").toUpperCase();
        if (code.length() != 4) {
            showError("Le code doit avoir 4 caractères (ex: AB3K)");
            return;
        }
        final String finalCode = code;
        connectToServer(() -> client.send("{\"type\":\"join\",\"room\":\"" + finalCode + "\"}"));
    }

    @FXML
    private void cancelWaiting() {
        if (client != null) { client.close(); client = null; }
        showModePanel();
    }

    @FXML
    private void showModePanel() {
        if (client != null) { client.close(); client = null; }
        modePanel.setVisible(true);
        waitingPanel.setVisible(false);
        errorPanel.setVisible(false);
    }

    @FXML
    private void goBack() {
        if (client != null) { client.close(); client = null; }
        switchScene("/com/supermorpion/menu-view.fxml");
    }

    // ── Connection logic ──────────────────────────────────────────────────────

    private void connectToServer(Runnable onConnected) {
        String url = serverUrlField.getText().trim();
        if (url.isBlank()) url = OnlineClient.DEFAULT_SERVER;

        showWaiting(false, "Connexion au serveur…");

        client = new OnlineClient();
        client.setOnOpen   (() -> Platform.runLater(onConnected));
        client.setOnMessage(msg -> Platform.runLater(() -> handleMessage(msg)));
        client.setOnClose  (r   -> Platform.runLater(() -> {
            if (!modePanel.isVisible()) showError("Connexion perdue");
        }));
        client.setOnError  (err -> Platform.runLater(() -> showError("Erreur : " + err)));

        client.connect(url);
    }

    private void handleMessage(String json) {
        String type = OnlineClient.getString(json, "type");
        if (type == null) return;
        switch (type) {
            case "created" -> {
                assignedPlayer = Player.X;
                String raw  = OnlineClient.getString(json, "room");
                String code = raw != null ? String.join(" ", raw.split("")) : "????";
                roomCodeLabel.setText(code);
                showWaiting(true, "En attente d'un adversaire…");
            }
            case "joined" -> {
                assignedPlayer = Player.O;
                showWaiting(false, "Connexion à la partie…");
            }
            case "start"   -> launchGame();
            case "error"   -> {
                String msg = OnlineClient.getString(json, "message");
                showError(msg != null ? msg : "Erreur inconnue");
            }
        }
    }

    private void launchGame() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/supermorpion/game-view.fxml"));
            Parent gameRoot = loader.load();
            GameViewController ctrl = loader.getController();
            ctrl.initOnlineMode(client, assignedPlayer);

            Stage stage = (Stage) lobbyRoot.getScene().getWindow();
            Scene scene = new Scene(gameRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());

            FadeTransition out = new FadeTransition(Duration.millis(200), lobbyRoot);
            out.setToValue(0);
            out.setOnFinished(e -> {
                stage.setScene(scene);
                FadeTransition in = new FadeTransition(Duration.millis(280), gameRoot);
                in.setFromValue(0); in.setToValue(1); in.play();
            });
            out.play();

        } catch (Exception ex) {
            showError("Erreur de chargement : " + ex.getMessage());
        }
    }

    // ── Panel helpers ─────────────────────────────────────────────────────────

    private void showWaiting(boolean withCode, String status) {
        modePanel.setVisible(false);
        errorPanel.setVisible(false);
        waitingPanel.setVisible(true);
        codeBox.setVisible(withCode);
        waitingStatusLabel.setText(status);
    }

    private void showError(String msg) {
        if (client != null) { client.close(); client = null; }
        modePanel.setVisible(false);
        waitingPanel.setVisible(false);
        errorPanel.setVisible(true);
        errorLabel.setText(msg);
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) lobbyRoot.getScene().getWindow();
            Scene scene = new Scene(root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());
            FadeTransition out = new FadeTransition(Duration.millis(200), lobbyRoot);
            out.setToValue(0);
            out.setOnFinished(e -> {
                stage.setScene(scene);
                FadeTransition in = new FadeTransition(Duration.millis(280), root);
                in.setFromValue(0); in.setToValue(1); in.play();
            });
            out.play();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
