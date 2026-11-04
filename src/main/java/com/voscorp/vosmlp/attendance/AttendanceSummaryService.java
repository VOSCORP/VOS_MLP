package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.journal.*;
import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceSummaryService {
    private final AttendanceAlertRepository alertRepo;
    private final AttendanceAlertService alertService;
    private final AttendanceCalc calc;
    private final AttendanceRecordRepository records;
    private final JournalColumnRepository columns;
    private final StudyGroupRepository groups;
    private final StudentRepository students;
    private final AppUserRepository users;

    public AttendanceSummaryService(AttendanceAlertRepository alertRepo, AttendanceAlertService alertService,
                                    AttendanceCalc calc, AttendanceRecordRepository records,
                                    JournalColumnRepository columns, StudyGroupRepository groups,
                                    StudentRepository students, AppUserRepository users) {
        this.alertRepo = alertRepo;
        this.alertService = alertService;
        this.calc = calc;
        this.records = records;
        this.columns = columns;
        this.groups = groups;
        this.students = students;
        this.users = users;
    }

    // аккаунт студента или старосты, которому можно написать письмо
    private Long accountId(Student student) {
        return users.findByLinkedStudent(student).stream()
                .filter(u -> u.getRole() == UserRole.STUDENT || u.getRole() == UserRole.STAROSTA)
                .map(AppUser::getId).findFirst().orElse(null);
    }

    private String mailBody(Student student, String details) {
        return "Здравствуйте, " + student.getFullName() + ". " + details
                + ". Пожалуйста, объясните, почему так получилось.";
    }

    public AttendanceSummary build() {
        // 1. история срабатываний
        List<AttendanceSummary.AlertRow> alertRows = new ArrayList<>();
        for (AttendanceAlert a : alertRepo.findTop50ByOrderByCreatedAtDesc()) {
            Student s = a.getStudent();
            alertRows.add(new AttendanceSummary.AlertRow(s.getFullName(),
                    s.getStudyGroup() == null ? "" : s.getStudyGroup().getName(),
                    a.getRule().getLabel(), a.getDetails(), a.getCreatedAtText(), accountId(s),
                    "Вопрос о посещаемости", mailBody(s, a.getDetails())));
        }

        // 2. кто превышает пороги сейчас
        List<AttendanceSummary.LiveRow> liveRows = new ArrayList<>();
        for (Student s : students.findAll()) {
            List<AlertTrigger> triggers = alertService.evaluate(s);
            if (triggers.isEmpty()) { continue; }
            List<String> lines = new ArrayList<>();
            for (AlertTrigger t : triggers) { lines.add(t.getText()); }
            liveRows.add(new AttendanceSummary.LiveRow(s.getFullName(), s.getStudyGroup().getName(), lines,
                    accountId(s), "Вопрос о посещаемости", mailBody(s, String.join("; ", lines))));
        }

        // 3. рейтинги за текущий период по каждой группе
        List<AttendanceSummary.LateRank> lateRanks = new ArrayList<>();
        List<AttendanceSummary.AbsenceRank> absenceRanks = new ArrayList<>();
        for (StudyGroup group : groups.findAll()) {
            LocalDate[] period = calc.currentPeriod(group);
            List<JournalColumn> lessons = columns.findByStudyGroup(group).stream()
                    .filter(c -> c.getType() == ColumnType.LESSON && calc.inPeriod(c, period))
                    .toList();
            if (lessons.isEmpty()) { continue; }
            List<AttendanceRecord> all = records.findByJournalColumnIn(lessons);

            // опоздания: предмет → студент → количество
            Map<String, Map<String, Integer>> latesBySubject = new TreeMap<>();
            // пропуски и записи по студентам
            Map<Student, List<AttendanceRecord>> byStudent = new LinkedHashMap<>();
            Map<Long, Student> studentById = new HashMap<>();
            for (AttendanceRecord r : all) {
                studentById.put(r.getStudent().getId(), r.getStudent());
            }
            for (AttendanceRecord r : all) {
                Student s = studentById.get(r.getStudent().getId());
                byStudent.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
                if (r.getMark() == AttendanceMark.LATE) {
                    String subject = r.getJournalColumn().getSubject().getName();
                    latesBySubject.computeIfAbsent(subject, k -> new HashMap<>())
                            .merge(r.getStudent().getFullName(), 1, Integer::sum);
                }
            }
            for (Map.Entry<String, Map<String, Integer>> e : latesBySubject.entrySet()) {
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(e.getValue().entrySet());
                sorted.sort((x, y) -> y.getValue() - x.getValue());
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                    lines.add((i + 1) + ". " + sorted.get(i).getKey() + ": " + sorted.get(i).getValue());
                }
                lateRanks.add(new AttendanceSummary.LateRank(group.getName(), e.getKey(), lines));
            }
            for (Map.Entry<Student, List<AttendanceRecord>> e : byStudent.entrySet()) {
                int absences = 0;
                int unexcused = 0;
                for (AttendanceRecord r : e.getValue()) {
                    if (r.getMark() == AttendanceMark.ABSENT) {
                        absences++;
                        if (!r.isExcused()) { unexcused++; }
                    }
                }
                if (absences > 0) {
                    int percent = calc.stats(e.getValue(), lessons.size())[2];
                    absenceRanks.add(new AttendanceSummary.AbsenceRank(e.getKey().getFullName(), group.getName(),
                            absences, unexcused, absences - unexcused, percent));
                }
            }
        }
        absenceRanks.sort((x, y) -> y.getAbsences() - x.getAbsences());
        List<AttendanceSummary.AbsenceRank> topAbsences =
                absenceRanks.subList(0, Math.min(10, absenceRanks.size()));

        return new AttendanceSummary(alertRows, liveRows, lateRanks, new ArrayList<>(topAbsences));
    }
}