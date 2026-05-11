package com.supermorpion.model;

import java.util.ArrayList;
import java.util.List;

public class GameBoard {

    private final SmallBoard[][] boards = new SmallBoard[3][3];
    private final Player[][] globalCells = new Player[3][3];
    private Player currentPlayer = Player.X;
    private int nextBoardRow = -1;
    private int nextBoardCol = -1;
    private Player globalWinner = Player.NONE;
    private boolean gameOver = false;

    public GameBoard() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                boards[r][c] = new SmallBoard();
                globalCells[r][c] = Player.NONE;
            }
    }

    public GameBoard(GameBoard other) {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                boards[r][c] = new SmallBoard(other.boards[r][c]);
                globalCells[r][c] = other.globalCells[r][c];
            }
        this.currentPlayer = other.currentPlayer;
        this.nextBoardRow = other.nextBoardRow;
        this.nextBoardCol = other.nextBoardCol;
        this.globalWinner = other.globalWinner;
        this.gameOver = other.gameOver;
    }

    public SmallBoard getSmallBoard(int row, int col) { return boards[row][col]; }
    public Player getGlobalCell(int row, int col) { return globalCells[row][col]; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public int getNextBoardRow() { return nextBoardRow; }
    public int getNextBoardCol() { return nextBoardCol; }
    public Player getGlobalWinner() { return globalWinner; }
    public boolean isGameOver() { return gameOver; }

    public boolean isValidMove(int boardRow, int boardCol, int cellRow, int cellCol) {
        if (gameOver) return false;
        if (boards[boardRow][boardCol].isFinished()) return false;
        if (!boards[boardRow][boardCol].isEmpty(cellRow, cellCol)) return false;
        if (nextBoardRow != -1 && (boardRow != nextBoardRow || boardCol != nextBoardCol)) return false;
        return true;
    }

    public void makeMove(int boardRow, int boardCol, int cellRow, int cellCol) {
        boards[boardRow][boardCol].setCell(cellRow, cellCol, currentPlayer);

        Player smallWinner = boards[boardRow][boardCol].getWinner();
        if (smallWinner != Player.NONE) {
            globalCells[boardRow][boardCol] = smallWinner;
        }

        globalWinner = computeGlobalWinner();
        if (globalWinner != Player.NONE || isAllBoardsFinished()) {
            gameOver = true;
            return;
        }

        if (boards[cellRow][cellCol].isFinished()) {
            nextBoardRow = -1;
            nextBoardCol = -1;
        } else {
            nextBoardRow = cellRow;
            nextBoardCol = cellCol;
        }
        currentPlayer = currentPlayer.opponent();
    }

    public List<int[]> getValidMoves() {
        List<int[]> moves = new ArrayList<>();
        if (gameOver) return moves;

        if (nextBoardRow != -1) {
            SmallBoard target = boards[nextBoardRow][nextBoardCol];
            for (int cr = 0; cr < 3; cr++)
                for (int cc = 0; cc < 3; cc++)
                    if (target.isEmpty(cr, cc))
                        moves.add(new int[]{nextBoardRow, nextBoardCol, cr, cc});
        } else {
            for (int br = 0; br < 3; br++)
                for (int bc = 0; bc < 3; bc++)
                    if (!boards[br][bc].isFinished())
                        for (int cr = 0; cr < 3; cr++)
                            for (int cc = 0; cc < 3; cc++)
                                if (boards[br][bc].isEmpty(cr, cc))
                                    moves.add(new int[]{br, bc, cr, cc});
        }
        return moves;
    }

    private Player computeGlobalWinner() {
        for (int i = 0; i < 3; i++) {
            if (globalCells[i][0] != Player.NONE && globalCells[i][0] == globalCells[i][1] && globalCells[i][1] == globalCells[i][2])
                return globalCells[i][0];
            if (globalCells[0][i] != Player.NONE && globalCells[0][i] == globalCells[1][i] && globalCells[1][i] == globalCells[2][i])
                return globalCells[0][i];
        }
        if (globalCells[0][0] != Player.NONE && globalCells[0][0] == globalCells[1][1] && globalCells[1][1] == globalCells[2][2])
            return globalCells[0][0];
        if (globalCells[0][2] != Player.NONE && globalCells[0][2] == globalCells[1][1] && globalCells[1][1] == globalCells[2][0])
            return globalCells[0][2];
        return Player.NONE;
    }

    private boolean isAllBoardsFinished() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (!boards[r][c].isFinished()) return false;
        return true;
    }
}
