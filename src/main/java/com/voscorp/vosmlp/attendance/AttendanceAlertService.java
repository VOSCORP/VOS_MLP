package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.journal.*;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.students.Student;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceAlertService {
    private final AttendanceCalc calc;
    private final AttendanceRecordRepository records;
    private final JournalColumnRepository columns;
    private final AttendanceAlertRepository alerts;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;
    private final int latesPerSubject;
    private final int absencesTotal;
    private final int minPercent;

    public AttendanceAlertService(AttendanceCalc calc, AttendanceRecordRepository records,
                                  JournalColumnRepository columns, AttendanceAlertRepository alerts,
                                  AppUserRepository users, NotificationService notifications, AuditService audit,
                                  @Value("${vos.attendance.lates-per-subject:7}") int latesPerSubject,
                                  @Value("${vos.attendance.absences-total:5}") int absencesTotal,
                                  @Value("${vos.attendance.min-percent:70}") int minPercent) {
        this.calc = calc;
        this.records = records;
        this.columns = columns;
        this.alerts = alerts;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
        this.latesPerSubject = latesPerSubject;
        this.absencesTotal = absencesTotal;
        this.minPercent = minPercent;
    }

    // Какие пороги студент превысил сейчас (за текущий период)
    public List<AlertTrigger> evaluate(Student student) {
        List<AlertTrigger> result = new ArrayList<>();
        StudyGroup group = student.getStudyGroup();
        if (group == null) { return result; }

        LocalDate[] period = calc.currentPeriod(group);
        LocalDate periodKey = period[0].equals(LocalDate.MIN) ? LocalDate.of(2000, 1, 1) : period[0];
        List<JournalColumn> lessons = columns.findByStudyGroup(group).stream()
                .filter(c -> c.getType() == ColumnType.LESSON && calc.inPeriod(c, period))
                .toList();
        if (lessons.isEmpty()) { return result; }

        List<AttendanceRecord> list = records.findByStudentAndJournalColumnIn(student, lessons);
        Map<Long, Integer> lates = new LinkedHashMap<>();
        Map<Long, String> subjectNames = new HashMap<>();
        int absences = 0;
        int unexcused = 0;
        for (AttendanceRecord r : list) {
            if (r.getMark() == AttendanceMark.LATE) {
                Subject s = r.getJournalColumn().getSubject();
                lates.merge(s.getId(), 1, Integer::sum);
                subjectNames.put(s.getId(), s.getName());
            } else {
                absences++;
                if (!r.isExcused()) { unexcused++; }
            }
        }

        for (Map.Entry<Long, Integer> e : lates.entrySet()) {
            if (e.getValue() >= latesPerSubject) {
                result.add(new AlertTrigger(AlertRule.LATES_IN_SUBJECT, e.getKey(),
                        "Опозданий по предмету «" + subjectNames.get(e.getKey()) + "»: " + e.getValue()
                                + " (порог " + latesPerSubject + ")", periodKey));
            }
        }
        if (absences > absencesTotal) {
            result.add(new AlertTrigger(AlertRule.ABSENCES_TOTAL, 0L,
                    "Пропусков за период: " + absences + " (без причины " + unexcused + ", с причиной "
                            + (absences - unexcused) + "); порог: больше " + absencesTotal, periodKey));
        }
        int percent = calc.stats(list, lessons.size())[2];
        if (percent < minPercent) {
            result.add(new AlertTrigger(AlertRule.LOW_ATTENDANCE, 0L,
                    "Посещаемость за период: " + percent + "% (ниже " + minPercent + "%)", periodKey));
        }
        return result;
    }

    // Вызывается после отметки посещаемости: новое срабатывание даёт одно уведомление всем админам
    public void check(Student student) {
        String groupName = student.getStudyGroup() == null ? "" : student.getStudyGroup().getName();
        for (AlertTrigger t : evaluate(student)) {
            if (alerts.existsByStudentAndRuleAndSubjectKeyAndPeriodStart(
                    student, t.getRule(), t.getSubjectKey(), t.getPeriodKey())) {
                continue;
            }
            alerts.save(new AttendanceAlert(student, t.getRule(), t.getSubjectKey(), t.getPeriodKey(), t.getText()));
            String text = "Порог посещаемости: " + student.getFullName() + " (" + groupName + "). " + t.getText()
                    + ". Требуется проверка: напишите студенту письмо.";
            for (AppUser admin : users.findByRole(UserRole.ADMIN)) {
                notifications.send(admin, text, "/admin/attendance");
            }
            audit.system(AuditAction.ALERT_ATTENDANCE, student.getFullName() + " · " + groupName, t.getText());
        }
    }
}