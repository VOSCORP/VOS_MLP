package com.voscorp.vosmlp.messages;

import java.util.List;

public enum LetterCategory {
    QUESTION("Вопрос", 4),
    EXPLANATION("Объяснение", 5),
    COMPLAINT("Жалоба", 6),
    STAROSTA_REMOVAL("Запрос на снятие старосты", 8),
    INFO("Информация", 2);

    private final String label;
    private final int defaultPriority;

    LetterCategory(String label, int defaultPriority) {
        this.label = label;
        this.defaultPriority = defaultPriority;
    }

    public String getLabel() { return label; }
    public int getDefaultPriority() { return defaultPriority; }

    // категории, которые выбирают преподаватели и админы
    public static List<LetterCategory> forStaff() {
        return List.of(QUESTION, EXPLANATION, COMPLAINT, INFO);
    }
}