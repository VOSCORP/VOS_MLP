package com.voscorp.vosmlp.journal;

// Короткое описание ячейки для аудит-лога: «было → стало»
public final class CellDescriber {
    private CellDescriber() {}

    public static String describe(JournalEntry e) {
        if (e == null) { return "—"; }
        if (e.getStatus() == CellStatus.MAKEUP) {
            return "отработка" + (e.getScore() != null ? " " + e.getScore() : "");
        }
        return e.getScore() == null ? "—" : String.valueOf(e.getScore());
    }
}