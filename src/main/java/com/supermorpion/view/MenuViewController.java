package com.supermorpion.view;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuViewController implements Initializable {

    @FXML private StackPane menuRoot;
    @FXML private VBox      card2P;
    @FXML private VBox      cardAI;
    @FXML private VBox      cardOnline;

    private static final Color COLOR_X      = Color.web("#00e5ff");
    private static final Color COLOR_IA     = Color.web("#ff1744");
    private static final Color COLOR_ONLINE = Color.web("#b040ff");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        menuRoot.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), menuRoot);
        ft.setToValue(1);
        ft.play();
    }

    // ── Hover effects ─────────────────────────────────────────────────────────

    @FXML private void hoverOn2P()     { applyHover(card2P,     COLOR_X);      }
    @FXML private void hoverOnAI()     { applyHover(cardAI,     COLOR_IA);     }
    @FXML private void hoverOnOnline() { applyHover(cardOnline, COLOR_ONLINE); }

    @FXML private void hoverOff() {
        removeHover(card2P);
        removeHover(cardAI);
        removeHover(cardOnline);
    }

    private void applyHover(VBox card, Color color) {
        card.setEffect(new DropShadow(28, color));
        ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
        st.setToX(1.04); st.setToY(1.04);
        st.play();
    }

    private void removeHover(VBox card) {
        card.setEffect(null);
        ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
        st.setToX(1.0); st.setToY(1.0);
        st.play();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void startTwoPlayers() { launchGame(false); }
    @FXML private void startVsAI()       { launchGame(true);  }

    @FXML private void startOnline() {
        switchScene("/com/supermorpion/online-lobby-view.fxml");
    }

    private void launchGame(boolean vsAI) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/supermorpion/game-view.fxml"));
            Parent gameRoot = loader.load();
            GameViewController ctrl = loader.getController();
            ctrl.initMode(vsAI);

            Stage stage    = (Stage) menuRoot.getScene().getWindow();
            Scene gameScene = new Scene(gameRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());

            fadeOut(menuRoot, () -> {
                stage.setScene(gameScene);
                fadeIn(gameRoot);
            });
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root  = loader.load();
            Stage  stage = (Stage) menuRoot.getScene().getWindow();
            Scene  scene = new Scene(root,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());
            fadeOut(menuRoot, () -> {
                stage.setScene(scene);
                fadeIn(root);
            });
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ── Transition helpers ────────────────────────────────────────────────────

    private void fadeOut(Parent node, Runnable onDone) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setToValue(0);
        ft.setOnFinished(e -> onDone.run());
        ft.play();
    }

    private void fadeIn(Parent node) {
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }
}
