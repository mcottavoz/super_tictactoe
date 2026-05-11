package com.supermorpion.view;

import com.supermorpion.ai.AIPlayer;
import com.supermorpion.model.GameBoard;
import com.supermorpion.model.Player;
import com.supermorpion.model.SmallBoard;
import com.supermorpion.online.OnlineClient;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class GameViewController implements Initializable {

    @FXML private VBox       playerXCard;
    @FXML private VBox       playerOCard;
    @FXML private Label      playerXName;
    @FXML private Label      playerOName;
    @FXML private Label      scoreXLabel;
    @FXML private Label      scoreOLabel;
    @FXML private Label      statusLabel;
    @FXML private GridPane   globalGrid;
    @FXML private BorderPane gameRoot;

    // ── State ─────────────────────────────────────────────────────────────────
    private GameBoard   gameBoard;
    private boolean     vsAI         = false;
    private AIPlayer    aiPlayer;
    private boolean     aiThinking   = false;
    private boolean     scoreUpdated = false;
    private int         scoreX       = 0;
    private int         scoreO       = 0;

    // ── Online mode ───────────────────────────────────────────────────────────
    private boolean      onlineMode   = false;
    private Player       localPlayer  = Player.NONE;
    private OnlineClient onlineClient;

    // ── Grid ──────────────────────────────────────────────────────────────────
    private final Pane[][]       smallBoardPanes = new Pane[3][3];
    private final Button[][][][] cellButtons     = new Button[3][3][3][3];
    private final Label[][]      overlayLabels   = new Label[3][3];
    private final boolean[][]    boardAnimated   = new boolean[3][3];

    private Timeline activeGlowTimeline;

    private static final Color COLOR_X      = Color.web("#00e5ff");
    private static final Color COLOR_O      = Color.web("#ff1744");
    private static final Color COLOR_ACTIVE = Color.web("#ffab00");

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildGrid();
        // Game is started by initMode() / initOnlineMode() from the calling controller
    }

    /** Local game (2 players or vs AI). */
    public void initMode(boolean vsAI) {
        this.vsAI      = vsAI;
        this.onlineMode = false;
        startNewGame();
    }

    /** Online game. Called by OnlineLobbyController after FXML load. */
    public void initOnlineMode(OnlineClient client, Player localPlayer) {
        this.onlineMode  = true;
        this.localPlayer = localPlayer;
        this.onlineClient = client;

        client.setOnMessage(msg -> Platform.runLater(() -> handleOnlineMessage(msg)));
        client.setOnClose  (r   -> Platform.runLater(this::handleDisconnect));
        client.setOnError  (err -> Platform.runLater(this::handleDisconnect));

        startNewGame();
    }

    // ── Grid construction ─────────────────────────────────────────────────────

    private void buildGrid() {
        globalGrid.setHgap(10);
        globalGrid.setVgap(10);
        globalGrid.setPadding(new Insets(14));
        globalGrid.setAlignment(Pos.CENTER);

        for (int br = 0; br < 3; br++)
            for (int bc = 0; bc < 3; bc++) {
                Pane p = createSmallBoardPane(br, bc);
                globalGrid.add(p, bc, br);
                smallBoardPanes[br][bc] = p;
            }
    }

    private Pane createSmallBoardPane(int br, int bc) {
        GridPane inner = new GridPane();
        inner.setHgap(4);
        inner.setVgap(4);
        inner.setPadding(new Insets(7));
        inner.setAlignment(Pos.CENTER);

        for (int cr = 0; cr < 3; cr++)
            for (int cc = 0; cc < 3; cc++) {
                final int fbr = br, fbc = bc, fcr = cr, fcc = cc;
                Button btn = new Button();
                btn.setPrefSize(54, 54);
                btn.setMinSize(54, 54);
                btn.getStyleClass().add("cell-button");
                btn.setOnAction(e -> handleCellClick(fbr, fbc, fcr, fcc));
                cellButtons[br][bc][cr][cc] = btn;
                inner.add(btn, cc, cr);
            }

        Label overlay = new Label();
        overlay.setFont(Font.font("Arial", FontWeight.BOLD, 84));
        overlay.setMouseTransparent(true);
        overlay.setVisible(false);
        overlayLabels[br][bc] = overlay;

        StackPane pane = new StackPane(inner, overlay);
        pane.getStyleClass().add("small-board-container");
        pane.setPrefSize(196, 196);
        return pane;
    }

    // ── Game logic ────────────────────────────────────────────────────────────

    private void handleCellClick(int br, int bc, int cr, int cc) {
        if (aiThinking) return;
        if (onlineMode && gameBoard.getCurrentPlayer() != localPlayer) return;
        if (!gameBoard.isValidMove(br, bc, cr, cc)) return;

        animateCellClick(cellButtons[br][bc][cr][cc]);
        gameBoard.makeMove(br, bc, cr, cc);

        if (onlineMode)
            onlineClient.send(OnlineClient.moveJson(br, bc, cr, cc));

        updateView();

        if (!onlineMode && !gameBoard.isGameOver() && vsAI && gameBoard.getCurrentPlayer() == Player.O)
            triggerAIMove();
    }

    private void triggerAIMove() {
        aiThinking = true;
        statusLabel.getStyleClass().removeAll("status-turn","status-win-x","status-win-o","status-draw");
        statusLabel.setText("IA RÉFLÉCHIT…");

        Thread t = new Thread(() -> {
            int[] move = aiPlayer.getBestMove(gameBoard);
            Platform.runLater(() -> {
                if (move != null) {
                    animateCellClick(cellButtons[move[0]][move[1]][move[2]][move[3]]);
                    gameBoard.makeMove(move[0], move[1], move[2], move[3]);
                }
                aiThinking = false;
                updateView();
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Online message handling ───────────────────────────────────────────────

    private void handleOnlineMessage(String json) {
        String type = OnlineClient.getString(json, "type");
        if (!"move".equals(type)) return;

        int br = OnlineClient.getInt(json, "boardRow");
        int bc = OnlineClient.getInt(json, "boardCol");
        int cr = OnlineClient.getInt(json, "cellRow");
        int cc = OnlineClient.getInt(json, "cellCol");

        if (br >= 0 && gameBoard.isValidMove(br, bc, cr, cc)) {
            animateCellClick(cellButtons[br][bc][cr][cc]);
            gameBoard.makeMove(br, bc, cr, cc);
            updateView();
        }
    }

    private void handleDisconnect() {
        stopGlowTimeline();
        statusLabel.getStyleClass().removeAll("status-turn","status-win-x","status-win-o","status-draw");
        statusLabel.setText("ADVERSAIRE PARTI");
        statusLabel.getStyleClass().add("status-win-o");
        disableAllCells();
    }

    private void disableAllCells() {
        for (int br = 0; br < 3; br++)
            for (int bc = 0; bc < 3; bc++)
                for (int cr = 0; cr < 3; cr++)
                    for (int cc = 0; cc < 3; cc++)
                        cellButtons[br][bc][cr][cc].setDisable(true);
    }

    // ── New game / reset ──────────────────────────────────────────────────────

    private void startNewGame() {
        stopGlowTimeline();
        gameBoard    = new GameBoard();
        if (!onlineMode)
            aiPlayer = vsAI ? new AIPlayer(Player.O) : null;
        aiThinking   = false;
        scoreUpdated = false;
        for (boolean[] row : boardAnimated) Arrays.fill(row, false);
        for (int br = 0; br < 3; br++)
            for (int bc = 0; bc < 3; bc++) {
                if (overlayLabels[br][bc] != null) {
                    overlayLabels[br][bc].setVisible(false);
                    overlayLabels[br][bc].setOpacity(0);
                }
                if (smallBoardPanes[br][bc] != null)
                    smallBoardPanes[br][bc].setEffect(null);
            }
        updateView();
    }

    // ── View update ───────────────────────────────────────────────────────────

    private void updateView() {
        for (int br = 0; br < 3; br++)
            for (int bc = 0; bc < 3; bc++)
                refreshSmallBoard(br, bc);
        refreshGlow();
        refreshPlayerCards();
    }

    private void refreshSmallBoard(int br, int bc) {
        SmallBoard sb = gameBoard.getSmallBoard(br, bc);
        for (int cr = 0; cr < 3; cr++)
            for (int cc = 0; cc < 3; cc++) {
                Button btn  = cellButtons[br][bc][cr][cc];
                Player cell = sb.getCell(cr, cc);
                btn.setText(cell.symbol());
                btn.getStyleClass().removeAll("x-cell","o-cell");
                if      (cell == Player.X) btn.getStyleClass().add("x-cell");
                else if (cell == Player.O) btn.getStyleClass().add("o-cell");
                btn.setDisable(!gameBoard.isValidMove(br, bc, cr, cc) || aiThinking);
            }

        if (boardAnimated[br][bc]) return;
        Player winner = sb.getWinner();
        if (winner != Player.NONE || sb.isFull()) {
            boardAnimated[br][bc] = true;
            Label overlay = overlayLabels[br][bc];
            if (winner != Player.NONE) {
                Color c = (winner == Player.X) ? COLOR_X : COLOR_O;
                overlay.setText(winner.symbol());
                overlay.setTextFill(c);
                overlay.setEffect(new DropShadow(20, c));
            } else {
                overlay.setText("=");
                overlay.setTextFill(Color.web("#445566"));
                overlay.setEffect(null);
            }
            animateOverlay(overlay);
        }
    }

    private void refreshGlow() {
        stopGlowTimeline();
        int nRow = gameBoard.getNextBoardRow();
        int nCol = gameBoard.getNextBoardCol();
        boolean specific = (nRow != -1);
        List<DropShadow> glows = new ArrayList<>();

        for (int br = 0; br < 3; br++)
            for (int bc = 0; bc < 3; bc++) {
                Pane pane = smallBoardPanes[br][bc];
                pane.getStyleClass().removeAll("active-board","inactive-board","finished-board");
                pane.setEffect(null);

                boolean finished = gameBoard.getSmallBoard(br, bc).isFinished();
                boolean active   = !finished && !gameBoard.isGameOver()
                                   && (!specific || (br == nRow && bc == nCol));

                if (finished || gameBoard.isGameOver()) {
                    pane.getStyleClass().add("finished-board");
                } else if (active) {
                    pane.getStyleClass().add("active-board");
                    if (specific) {
                        DropShadow ds = new DropShadow(10, COLOR_ACTIVE);
                        pane.setEffect(ds);
                        glows.add(ds);
                    }
                } else {
                    pane.getStyleClass().add("inactive-board");
                }
            }

        if (!glows.isEmpty()) {
            List<KeyValue> kv0 = new ArrayList<>();
            List<KeyValue> kv1 = new ArrayList<>();
            for (DropShadow ds : glows) {
                kv0.add(new KeyValue(ds.radiusProperty(), Double.valueOf(8)));
                kv1.add(new KeyValue(ds.radiusProperty(), Double.valueOf(28)));
            }
            activeGlowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        kv0.toArray(new KeyValue[0])),
                new KeyFrame(Duration.millis(900), kv1.toArray(new KeyValue[0]))
            );
            activeGlowTimeline.setCycleCount(Timeline.INDEFINITE);
            activeGlowTimeline.setAutoReverse(true);
            activeGlowTimeline.play();
        }
    }

    private void refreshPlayerCards() {
        // Names
        if (onlineMode) {
            playerXName.setText(localPlayer == Player.X ? "Vous" : "Adversaire");
            playerOName.setText(localPlayer == Player.O ? "Vous" : "Adversaire");
        } else {
            playerXName.setText(vsAI ? "Vous" : "Joueur 1");
            playerOName.setText(vsAI ? "IA"   : "Joueur 2");
        }

        // Score
        if (gameBoard.isGameOver() && !scoreUpdated) {
            scoreUpdated = true;
            Player w = gameBoard.getGlobalWinner();
            if      (w == Player.X) scoreX++;
            else if (w == Player.O) scoreO++;
        }
        scoreXLabel.setText(String.valueOf(scoreX));
        scoreOLabel.setText(String.valueOf(scoreO));

        playerXCard.getStyleClass().removeAll("turn-x","turn-o");
        playerOCard.getStyleClass().removeAll("turn-x","turn-o");
        statusLabel.getStyleClass().removeAll("status-turn","status-win-x","status-win-o","status-draw");

        if (gameBoard.isGameOver()) {
            Player w = gameBoard.getGlobalWinner();
            if (w == Player.X) {
                playerXCard.getStyleClass().add("turn-x");
                statusLabel.setText("VICTOIRE !");
                statusLabel.getStyleClass().add("status-win-x");
            } else if (w == Player.O) {
                playerOCard.getStyleClass().add("turn-o");
                statusLabel.setText("VICTOIRE !");
                statusLabel.getStyleClass().add("status-win-o");
            } else {
                statusLabel.setText("ÉGALITÉ");
                statusLabel.getStyleClass().add("status-draw");
            }
        } else if (aiThinking) {
            playerOCard.getStyleClass().add("turn-o");
        } else {
            Player curr = gameBoard.getCurrentPlayer();
            if (curr == Player.X) {
                playerXCard.getStyleClass().add("turn-x");
                statusLabel.setText(onlineMode
                    ? (localPlayer == Player.X ? "VOTRE TOUR"      : "ADVERSAIRE JOUE…")
                    : (vsAI ? "VOTRE TOUR" : "TOUR DE X"));
            } else {
                playerOCard.getStyleClass().add("turn-o");
                statusLabel.setText(onlineMode
                    ? (localPlayer == Player.O ? "VOTRE TOUR"      : "ADVERSAIRE JOUE…")
                    : (vsAI ? "IA JOUE" : "TOUR DE O"));
            }
            statusLabel.getStyleClass().add("status-turn");
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────

    private void animateCellClick(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(0.76);  st.setToY(0.76);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void animateOverlay(Label overlay) {
        overlay.setScaleX(1.9); overlay.setScaleY(1.9);
        overlay.setOpacity(0);  overlay.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(380), overlay);
        ft.setFromValue(0); ft.setToValue(0.86);
        ft.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition st = new ScaleTransition(Duration.millis(380), overlay);
        st.setFromX(1.9); st.setFromY(1.9);
        st.setToX(1.0);   st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, st).play();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stopGlowTimeline() {
        if (activeGlowTimeline != null) { activeGlowTimeline.stop(); activeGlowTimeline = null; }
    }

    // ── FXML actions ──────────────────────────────────────────────────────────

    @FXML
    private void onNewGame() {
        if (aiThinking) return;
        if (onlineMode) { onGoToMenu(); return; }
        startNewGame();
    }

    @FXML
    private void onGoToMenu() {
        if (aiThinking) return;
        if (onlineMode && onlineClient != null) onlineClient.close();
        stopGlowTimeline();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/supermorpion/menu-view.fxml"));
            Parent menuRoot = loader.load();
            Stage stage = (Stage) gameRoot.getScene().getWindow();
            Scene scene  = new Scene(menuRoot,
                    stage.getScene().getWidth(),
                    stage.getScene().getHeight());
            FadeTransition out = new FadeTransition(Duration.millis(200), gameRoot);
            out.setToValue(0);
            out.setOnFinished(e -> {
                stage.setScene(scene);
                FadeTransition in = new FadeTransition(Duration.millis(280), menuRoot);
                in.setFromValue(0); in.setToValue(1); in.play();
            });
            out.play();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
