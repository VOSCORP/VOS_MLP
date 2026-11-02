package com.voscorp.vosmlp.discipline;

import com.voscorp.vosmlp.students.Student;
import com.voscorp.vosmlp.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class DisciplineSeed implements CommandLineRunner {
    private final DisciplinaryNoticeRepository notices;
    private final ExpulsionProposalRepository proposals;
    private final StudentRepository students;

    public DisciplineSeed(DisciplinaryNoticeRepository notices, ExpulsionProposalRepository proposals,
                          StudentRepository students) {
        this.notices = notices;
        this.proposals = proposals;
        this.students = students;
    }

    @Override
    public void run(String... args) {
        if (notices.count() > 0) { return; }
        Student karimov = students.findAll().stream()
                .filter(s -> s.getFullName().equals("Каримов Тимур")).findFirst().orElse(null);
        if (karimov == null) { return; }
        for (int i = 1; i <= 4; i++) {
            notices.save(new DisciplinaryNotice(karimov, null, "Демо: документ не прошёл проверку (" + i + ")"));
        }
        proposals.save(new ExpulsionProposal(karimov, 4));
    }
}