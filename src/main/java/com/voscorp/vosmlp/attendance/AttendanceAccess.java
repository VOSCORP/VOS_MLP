package com.voscorp.vosmlp.attendance;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.journal.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceAccess {
    private final JournalAccess journalAccess;
    private final TeachingAssignmentRepository assignments;

    public AttendanceAccess(JournalAccess journalAccess, TeachingAssignmentRepository assignments) {
        this.journalAccess = journalAccess;
        this.assignments = assignments;
    }

    private StudyGroup ownGroup(AppUser user) {
        return user.getLinkedStudent() == null ? null : user.getLinkedStudent().getStudyGroup();
    }

    private boolean isOwnGroup(AppUser user, StudyGroup group) {
        StudyGroup own = ownGroup(user);
        return own != null && own.getId().equals(group.getId());
    }

    // какие группы видит пользователь (староста видит только свою)
    public List<StudyGroup> visibleGroups(AppUser user) {
        if (user.getRole() == UserRole.STAROSTA) {
            StudyGroup own = ownGroup(user);
            return own == null ? List.of() : List.of(own);
        }
        return journalAccess.visibleGroups(user);
    }

    public List<Subject> visibleSubjects(AppUser user, StudyGroup group) {
        return journalAccess.visibleSubjects(user, group);
    }

    // можно ли вообще отмечать в этой таблице (для интерфейса)
    public boolean canMarkAny(AppUser user, StudyGroup group, Subject subject) {
        switch (user.getRole()) {
            case ADMIN:
                return true;
            case TEACHER:
                return assignments.existsByTeacherAndStudyGroupAndSubject(user, group, subject);
            case STAROSTA:
                return isOwnGroup(user, group);
            default:
                return false;
        }
    }

    // проверка конкретной отметки: текст ошибки или null, если можно
    public String checkMark(AppUser user, JournalColumn column) {
        if (column.getType() != ColumnType.LESSON) {
            return "Посещаемость отмечается только на обычных занятиях";
        }
        StudyGroup group = column.getStudyGroup();
        switch (user.getRole()) {
            case ADMIN:
                return null;
            case TEACHER:
                return assignments.existsByTeacherAndStudyGroupAndSubject(user, group, column.getSubject())
                        ? null : "Вы отмечаете посещаемость только по своим предметам и группам";
            case STAROSTA: {
                if (!isOwnGroup(user, group)) {
                    return "Староста отмечает посещаемость только своей группы";
                }
                LocalDate date = column.getColumnDate();
                LocalDate today = LocalDate.now();
                if (!date.equals(today) && !date.equals(today.minusDays(1))) {
                    return "Староста может отмечать только сегодняшние и вчерашние занятия. За другие даты обратитесь к преподавателю.";
                }
                return null;
            }
            default:
                return "Посещаемость открыта только для просмотра";
        }
    }
}