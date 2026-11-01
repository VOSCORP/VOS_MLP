package com.voscorp.vosmlp.journal;

// Короткое описание ячейки для аудит-лога: «было → стало»
public final class CellDescriber {
    private CellDescriber() {}

    public static String describe(JournalEntry e) {
        if (e == null) { return "—"; }
        switch (e.getStatus()) {
            case LATE:
                return (e.getScore() != null ? e.getScore() + " · " : "") + "опоздание " + e.getLateMinutes() + " мин";
            case ABSENT:
                return e.isExcused() ? "пропуск (с причиной)" : "пропуск (без причины)";
            case MAKEUP:
                return "отработка" + (e.getScore() != null ? " " + e.getScore() : "");
            default:
                return e.getScore() == null ? "—" : String.valueOf(e.getScore());
        }
    }
}