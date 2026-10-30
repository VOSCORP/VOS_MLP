package com.voscorp.vosmlp.journal;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.students.Student;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JournalAccess {
    private final StudyGroupRepository groups;
    private final JournalColumnRepository columns;
    private final TeachingAssignmentRepository assignments;

    public JournalAccess(StudyGroupRepository groups, JournalColumnRepository columns,
                         TeachingAssignmentRepository assignments) {
        this.groups = groups;
        this.columns = columns;
        this.assignments = assignments;
    }

    private boolean isCurator(AppUser user, StudyGroup group) {
        return group.getCurator() != null && group.getCurator().getId().equals(user.getId());
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
            default: {
                Student s = user.getLinkedStudent();
                if (s != null && s.getStudyGroup() != null) {
                    return List.of(s.getStudyGroup());
                }
                return List.of();
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
        Map<Long, Subject> all = new LinkedHashMap<>();
        for (JournalColumn c : columns.findByStudyGroup(group)) {
            all.put(c.getSubject().getId(), c.getSubject());
        }
        return new ArrayList<>(all.values());
    }

    // кто может править ячейки (староста получит это право в шаге 5д)
    public boolean canEdit(AppUser user, StudyGroup group, Subject subject) {
        if (user.getRole() == UserRole.ADMIN) { return true; }
        if (user.getRole() == UserRole.TEACHER) {
            return assignments.existsByTeacherAndStudyGroupAndSubject(user, group, subject);
        }
        return false;
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