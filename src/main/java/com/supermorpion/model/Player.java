package com.supermorpion.model;

public enum Player {
    X, O, NONE;

    public Player opponent() {
        return switch (this) {
            case X -> O;
            case O -> X;
            case NONE -> NONE;
        };
    }

    public String symbol() {
        return switch (this) {
            case X -> "X";
            case O -> "O";
            case NONE -> "";
        };
    }
}
