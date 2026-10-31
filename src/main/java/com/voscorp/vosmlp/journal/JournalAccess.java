package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JournalAccess {
    private final StudyGroupRepository groups;
    private final JournalColumnRepository columns;
    private final TeachingAssignmentRepository assignments;
    private final JournalGrantRepository grants;

    public JournalAccess(StudyGroupRepository groups, JournalColumnRepository columns,
                         TeachingAssignmentRepository assignments, JournalGrantRepository grants) {
        this.groups = groups;
        this.columns = columns;
        this.assignments = assignments;
        this.grants = grants;
    }

    private boolean isCurator(AppUser user, StudyGroup group) {
        return group.getCurator() != null && group.getCurator().getId().equals(user.getId());
    }

    private StudyGroup ownGroup(AppUser user) {
        Student s = user.getLinkedStudent();
        return s == null ? null : s.getStudyGroup();
    }

    private boolean isOwnGroup(AppUser user, StudyGroup group) {
        StudyGroup own = ownGroup(user);
        return own != null && own.getId().equals(group.getId());
    }

    private List<JournalGrant> liveGrants(AppUser starosta) {
        List<JournalGrant> result = new ArrayList<>();
        for (JournalGrant g : grants.findByStarosta(starosta)) {
            if (g.isLive()) { result.add(g); }
        }
        return result;
    }

    // действующие доступы старосты на конкретную группу и предмет
    private List<JournalGrant> grantsFor(AppUser starosta, StudyGroup group, Subject subject) {
        List<JournalGrant> result = new ArrayList<>();
        for (JournalGrant g : liveGrants(starosta)) {
            if (!g.getStudyGroup().getId().equals(group.getId())) { continue; }
            for (Subject s : g.getSubjects()) {
                if (s.getId().equals(subject.getId())) {
                    result.add(g);
                    break;
                }
            }
        }
        return result;
    }

    // какие группы пользователь вообще видит
    public List<StudyGroup> visibleGroups(AppUser user) {
        switch (user.getRole()) {
            case ADMIN:
                return groups.findAll();
            case TEACHER: {
                Map<Long, StudyGroup> map = new LinkedHashMap<>();
                for (TeachingAssignment a : assignments.findByTeacher(user)) {
                    map.put(a.getStudyGroup().getId(), a.getStudyGroup());
                }
                for (StudyGroup g : groups.findByCurator(user)) {
                    map.put(g.getId(), g);
                }
                return new ArrayList<>(map.values());
            }
            case STAROSTA: {
                Map<Long, StudyGroup> map = new LinkedHashMap<>();
                StudyGroup own = ownGroup(user);
                if (own != null) { map.put(own.getId(), own); }
                for (JournalGrant g : liveGrants(user)) {
                    map.put(g.getStudyGroup().getId(), g.getStudyGroup());
                }
                return new ArrayList<>(map.values());
            }
            default: {
                StudyGroup own = ownGroup(user);
                return own == null ? List.of() : List.of(own);
            }
        }
    }

    // какие предметы пользователь видит в группе
    public List<Subject> visibleSubjects(AppUser user, StudyGroup group) {
        if (user.getRole() == UserRole.TEACHER && !isCurator(user, group)) {
            Map<Long, Subject> own = new LinkedHashMap<>();
            for (TeachingAssignment a : assignments.findByTeacherAndStudyGroup(user, group)) {
                own.put(a.getSubject().getId(), a.getSubject());
            }
            return new ArrayList<>(own.values());
        }
        if (user.getRole() == UserRole.STAROSTA && !isOwnGroup(user, group)) {
            // в чужой группе староста видит только предметы, на которые ему дали доступ
            Map<Long, Subject> granted = new LinkedHashMap<>();
            for (JournalGrant g : liveGrants(user)) {
                if (g.getStudyGroup().getId().equals(group.getId())) {
                    for (Subject s : g.getSubjects()) { granted.put(s.getId(), s); }
                }
            }
            return new ArrayList<>(granted.values());
        }
        Map<Long, Subject> all = new LinkedHashMap<>();
        for (JournalColumn c : columns.findByStudyGroup(group)) {
            all.put(c.getSubject().getId(), c.getSubject());
        }
        return new ArrayList<>(all.values());
    }

    // можно ли вообще править эту таблицу (сами правила по ячейкам ниже)
    public boolean canEdit(AppUser user, StudyGroup group, Subject subject) {
        switch (user.getRole()) {
            case ADMIN:
                return true;
            case TEACHER:
                return assignments.existsByTeacherAndStudyGroupAndSubject(user, group, subject);
            case STAROSTA:
                return isOwnGroup(user, group) || !grantsFor(user, group, subject).isEmpty();
            default:
                return false;
        }
    }

    // Проверка правки конкретной ячейки. Возвращает текст ошибки или null, если можно.
    public String checkCellEdit(AppUser user, JournalColumn column, JournalEntry existing,
                                CellStatus newStatus, boolean makesEmpty) {
        StudyGroup group = column.getStudyGroup();
        Subject subject = column.getSubject();

        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.TEACHER) {
            return canEdit(user, group, subject) ? null : "У вас нет прав на правку этого журнала";
        }
        if (user.getRole() != UserRole.STAROSTA) {
            return "Журнал открыт только для просмотра";
        }

        // 1) отметки о посещаемости в своей группе в день занятия: разрешение не нужно
        boolean today = column.getColumnDate().equals(LocalDate.now());
        boolean attendanceMark = newStatus == CellStatus.LATE || newStatus == CellStatus.ABSENT;
        boolean existingMark = existing != null
                && (existing.getStatus() == CellStatus.LATE || existing.getStatus() == CellStatus.ABSENT);
        if (isOwnGroup(user, group) && today) {
            if (attendanceMark && (existing == null || existingMark)) { return null; }
            if (makesEmpty && existingMark) { return null; }
        }

        // 2) всё остальное только по доступу от преподавателя
        List<JournalGrant> matching = grantsFor(user, group, subject);
        if (matching.isEmpty()) {
            return "Нужен доступ от преподавателя. Без него вы можете отмечать только опоздания и пропуски своей группы в день занятия.";
        }
        if (makesEmpty) { return "Староста не может удалять оценки."; }

        boolean canFill = false;
        boolean canChange = false;
        for (JournalGrant g : matching) {
            if (g.getAccessType() != AccessType.CHANGE) { canFill = true; }
            if (g.getAccessType() != AccessType.FILL) { canChange = true; }
        }
        if (existing == null && !canFill) { return "Доступ «Изменить»: заполнять пустые ячейки нельзя."; }
        if (existing != null && !canChange) { return "Доступ «Заполнить»: менять уже поставленные оценки нельзя."; }
        return null;
    }

    // кто видит подробности про стипендию по предметам
    public boolean canSeeScholarshipDetails(AppUser user, Student student) {
        switch (user.getRole()) {
            case ADMIN:
                return true;
            case TEACHER:
                return student.getStudyGroup() != null && isCurator(user, student.getStudyGroup());
            default:
                return user.getLinkedStudent() != null && user.getLinkedStudent().getId().equals(student.getId());
        }
    }
}