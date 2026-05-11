package com.supermorpion.ai;

import com.supermorpion.model.GameBoard;
import com.supermorpion.model.Player;
import com.supermorpion.model.SmallBoard;

import java.util.List;

public class AIPlayer {

    private static final int WIN_SCORE  = 1_000_000;
    private static final int[] POSITION_WEIGHT = {2, 1, 2, 1, 3, 1, 2, 1, 2};

    private final Player ai;
    private final Player human;

    public AIPlayer(Player aiPlayer) {
        this.ai = aiPlayer;
        this.human = aiPlayer.opponent();
    }

    public int[] getBestMove(GameBoard board) {
        List<int[]> moves = board.getValidMoves();
        if (moves.isEmpty()) return null;

        int depth = adaptiveDepth(moves.size());
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = moves.get(0);

        for (int[] move : moves) {
            GameBoard copy = new GameBoard(board);
            copy.makeMove(move[0], move[1], move[2], move[3]);
            int score = minimax(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int adaptiveDepth(int moveCount) {
        if (moveCount <= 9)  return 7;
        if (moveCount <= 18) return 6;
        if (moveCount <= 36) return 5;
        return 4;
    }

    private int minimax(GameBoard board, int depth, int alpha, int beta, boolean maximizing) {
        if (board.isGameOver() || depth == 0) {
            return evaluate(board);
        }

        List<int[]> moves = board.getValidMoves();
        if (moves.isEmpty()) return evaluate(board);

        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int[] move : moves) {
                GameBoard copy = new GameBoard(board);
                copy.makeMove(move[0], move[1], move[2], move[3]);
                best = Math.max(best, minimax(copy, depth - 1, alpha, beta, false));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int[] move : moves) {
                GameBoard copy = new GameBoard(board);
                copy.makeMove(move[0], move[1], move[2], move[3]);
                best = Math.min(best, minimax(copy, depth - 1, alpha, beta, true));
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }

    private int evaluate(GameBoard board) {
        Player winner = board.getGlobalWinner();
        if (winner == ai)    return WIN_SCORE;
        if (winner == human) return -WIN_SCORE;

        int score = evaluateGlobalLines(board);

        for (int br = 0; br < 3; br++) {
            for (int bc = 0; bc < 3; bc++) {
                int posWeight = POSITION_WEIGHT[br * 3 + bc];
                score += evaluateSmallBoard(board.getSmallBoard(br, bc), posWeight);
            }
        }
        return score;
    }

    private int evaluateGlobalLines(GameBoard board) {
        int score = 0;
        int[][] lines = {
            {0,0, 0,1, 0,2}, {1,0, 1,1, 1,2}, {2,0, 2,1, 2,2},
            {0,0, 1,0, 2,0}, {0,1, 1,1, 2,1}, {0,2, 1,2, 2,2},
            {0,0, 1,1, 2,2}, {0,2, 1,1, 2,0}
        };
        for (int[] l : lines) {
            score += scoreLine(
                board.getGlobalCell(l[0], l[1]),
                board.getGlobalCell(l[2], l[3]),
                board.getGlobalCell(l[4], l[5]),
                50
            );
        }
        return score;
    }

    private int evaluateSmallBoard(SmallBoard board, int weight) {
        Player winner = board.getWinner();
        if (winner == ai)    return 30 * weight;
        if (winner == human) return -30 * weight;
        if (board.isFull())  return 0;

        int score = 0;
        int[][] lines = {
            {0,0, 0,1, 0,2}, {1,0, 1,1, 1,2}, {2,0, 2,1, 2,2},
            {0,0, 1,0, 2,0}, {0,1, 1,1, 2,1}, {0,2, 1,2, 2,2},
            {0,0, 1,1, 2,2}, {0,2, 1,1, 2,0}
        };
        for (int[] l : lines) {
            score += scoreLine(
                board.getCell(l[0], l[1]),
                board.getCell(l[2], l[3]),
                board.getCell(l[4], l[5]),
                weight
            );
        }
        return score;
    }

    private int scoreLine(Player a, Player b, Player c, int weight) {
        int aiCount = 0, humanCount = 0;
        for (Player p : new Player[]{a, b, c}) {
            if (p == ai)    aiCount++;
            else if (p == human) humanCount++;
        }
        if (aiCount > 0 && humanCount > 0) return 0;
        if (aiCount == 2)    return  5 * weight;
        if (humanCount == 2) return -5 * weight;
        if (aiCount == 1)    return      weight;
        if (humanCount == 1) return     -weight;
        return 0;
    }
}
