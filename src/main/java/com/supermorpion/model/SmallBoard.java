package com.supermorpion.model;

public class SmallBoard {

    private final Player[][] cells = new Player[3][3];
    private Player winner = Player.NONE;

    public SmallBoard() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                cells[r][c] = Player.NONE;
    }

    public SmallBoard(SmallBoard other) {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                cells[r][c] = other.cells[r][c];
        this.winner = other.winner;
    }

    public Player getCell(int row, int col) {
        return cells[row][col];
    }

    public void setCell(int row, int col, Player player) {
        cells[row][col] = player;
        if (winner == Player.NONE) {
            winner = computeWinner();
        }
    }

    public Player getWinner() {
        return winner;
    }

    public boolean isFinished() {
        return winner != Player.NONE || isFull();
    }

    public boolean isFull() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (cells[r][c] == Player.NONE) return false;
        return true;
    }

    public boolean isEmpty(int row, int col) {
        return cells[row][col] == Player.NONE;
    }

    private Player computeWinner() {
        for (int i = 0; i < 3; i++) {
            if (cells[i][0] != Player.NONE && cells[i][0] == cells[i][1] && cells[i][1] == cells[i][2])
                return cells[i][0];
            if (cells[0][i] != Player.NONE && cells[0][i] == cells[1][i] && cells[1][i] == cells[2][i])
                return cells[0][i];
        }
        if (cells[0][0] != Player.NONE && cells[0][0] == cells[1][1] && cells[1][1] == cells[2][2])
            return cells[0][0];
        if (cells[0][2] != Player.NONE && cells[0][2] == cells[1][1] && cells[1][1] == cells[2][0])
            return cells[0][2];
        return Player.NONE;
    }
}
