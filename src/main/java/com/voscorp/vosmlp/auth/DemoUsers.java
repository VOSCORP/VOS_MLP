package com.voscorp.vosmlp.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class DemoUsers implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public DemoUsers(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) { return; }
        users.save(new AppUser("admin", encoder.encode("demo123"), "Администратор системы", UserRole.ADMIN));
        users.save(new AppUser("teacher1", encoder.encode("demo123"), "Асанов Бауыржан", UserRole.TEACHER));
        users.save(new AppUser("student1", encoder.encode("demo123"), "Иванов Алихан", UserRole.STUDENT));
        users.save(new AppUser("parent1", encoder.encode("demo123"), "Иванова Гульмира", UserRole.PARENT));
        users.save(new AppUser("starosta1", encoder.encode("demo123"), "Омаров Ерлан", UserRole.STAROSTA));
    }
}