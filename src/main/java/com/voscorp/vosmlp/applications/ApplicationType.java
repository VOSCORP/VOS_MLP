package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.signatures.SignAction;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum ApplicationType {
    ATTENDANCE_CHANGE("Изменение посещаемости",
            "Закрыть пропуски без причины справкой. Рассматривают администраторы.",
            SignAction.ATTENDANCE_CHANGE, true, "/applications/attendance/new",
            UserRole.STUDENT, UserRole.STAROSTA),
    EDUCATION_CERT("Справка об образовании",
            "Справка о том, что вы учитесь в колледже. Выдаётся сразу, без администрации.",
            null, false, "", UserRole.STUDENT, UserRole.STAROSTA),
    DISPUTE("Открытие спора",
            "Спор об оценке или о посещаемости.",
            null, false, "", UserRole.STUDENT, UserRole.STAROSTA),
    JOURNAL_ACCESS("Управление журналом",
            "Заявка преподавателю на доступ к журналу (с подписью).",
            null, false, "", UserRole.STAROSTA),
    MATERNITY_LEAVE("Декретный отпуск",
            "Заявка с подтверждением из Medical System.",
            null, false, "", UserRole.TEACHER);

    private final String label;
    private final String description;
    private final SignAction signAction; // null: подпись не нужна
    private final boolean ready;         // false: показываем «Скоро»
    private final String formPath;
    private final Set<UserRole> roles;

    ApplicationType(String label, String description, SignAction signAction, boolean ready,
                    String formPath, UserRole... roles) {
        this.label = label;
        this.description = description;
        this.signAction = signAction;
        this.ready = ready;
        this.formPath = formPath;
        this.roles = EnumSet.copyOf(Arrays.asList(roles));
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public SignAction getSignAction() { return signAction; }
    public boolean isReady() { return ready; }
    public String getFormPath() { return formPath; }
    public Set<UserRole> getRoles() { return roles; }

    public static List<ApplicationType> availableFor(UserRole role) {
        return Arrays.stream(values()).filter(t -> t.roles.contains(role)).toList();
    }
}