package com.voscorp.vosmlp.audit;

public enum AuditAction {
    LOGIN("Вход в систему"),
    LOGIN_FAILED("Неудачный вход"),
    GRADE_SET("Оценка поставлена"),
    GRADE_CHANGED("Оценка изменена"),
    CELL_CLEARED("Ячейка очищена"),
    ATTENDANCE_MARK("Отметка посещаемости"),
    ACCESS_DENIED("Отказ в доступе"),
    ACCESS_GRANTED("Доступ старосте выдан"),
    ACCESS_DONE("Староста выполнил работу"),
    ACCESS_CLOSED("Доступ старосте закрыт"),
    ACCESS_EXPIRED("Доступ закрыт автоматически"),
    DOC_VERIFIED("Документ проверен (Medical API)"),
    DOC_APPROVED("Заявление одобрено"),
    DOC_REJECTED("Заявление отклонено"),
    NOTICE_CREATED("Дисциплинарное замечание"),
    EXPULSION_PROPOSED("Предложение об отчислении"),
    EXPULSION_CONFIRMED("Отчисление подтверждено"),
    EXPULSION_REJECTED("Отчисление отклонено"),
    DOCUMENT_UPLOADED("Документ загружен"),
    DOCUMENT_REMOVED("Документ удалён"),
    DOCUMENT_SYNCED("Документ сверен с VOS Gov"),
    LETTER_SENT("Письмо отправлено"),
    ALERT_ATTENDANCE("Сработал порог посещаемости");

    private final String label;

    AuditAction(String label) { this.label = label; }

    public String getLabel() { return label; }
}