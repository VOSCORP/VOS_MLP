package com.voscorp.vosmlp.messages;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(7)
public class LetterSeed implements CommandLineRunner {
    private final LetterThreadRepository threads;
    private final AppUserRepository users;
    private final LetterService service;

    public LetterSeed(LetterThreadRepository threads, AppUserRepository users, LetterService service) {
        this.threads = threads;
        this.users = users;
        this.service = service;
    }

    @Override
    public void run(String... args) {
        if (threads.count() > 0) { return; }
        AppUser admin = users.findByUsername("admin").orElse(null);
        AppUser student1 = users.findByUsername("student1").orElse(null);
        if (admin == null || student1 == null) { return; }

        service.sendFromSystem(student1, "Добро пожаловать в VOS College",
                "Здесь вы будете получать письма от преподавателей, администрации и системы. На письма от системы отвечать не нужно.");
        service.send(admin, student1.getId(), "Вопрос о посещаемости",
                "Здравствуйте, Алихан. За текущий период у вас есть пропуск без причины и опоздание. "
                        + "Пожалуйста, объясните, что произошло.");
    }
}