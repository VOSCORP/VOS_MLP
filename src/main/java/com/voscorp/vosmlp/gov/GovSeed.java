package com.voscorp.vosmlp.gov;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.documents.DocType;
import com.voscorp.vosmlp.documents.PdfFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(8)
public class GovSeed implements CommandLineRunner {
    private final GovDocumentRepository repo;
    private final AppUserRepository users;

    public GovSeed(GovDocumentRepository repo, AppUserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    private void add(String username, DocType type, String number, String issuer,
                     LocalDate issued, LocalDate expiry, int version) {
        AppUser owner = users.findByUsername(username).orElse(null);
        if (owner == null) { return; }
        byte[] pdf = PdfFactory.simple(type.name().replace('_', ' '), List.of(
                "Holder: " + PdfFactory.translit(owner.getFullName()),
                "Number: " + number,
                "Issued by: " + PdfFactory.translit(issuer),
                "Issued: " + issued,
                "Valid until: " + (expiry == null ? "unlimited" : expiry.toString()),
                "VOS Gov version: " + version));
        repo.save(new GovDocument(owner, type, number, issuer, issued, expiry, version,
                type.name().toLowerCase().replace('_', '-') + "-" + number + ".pdf", pdf));
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) { return; }
        LocalDate d = LocalDate.of(2022, 3, 15);

        add("student1", DocType.ID_CARD, "041234567", "МВД РК", d, LocalDate.of(2032, 3, 14), 1);
        // в Gov версия новее (в колледже будет старая копия)
        add("student1", DocType.FOREIGN_PASSPORT, "N12345678", "МВД РК",
                LocalDate.of(2026, 9, 1), LocalDate.of(2036, 8, 31), 2);
        add("student1", DocType.BIRTH_CERTIFICATE, "BC-778899", "ЗАГС", LocalDate.of(2007, 4, 2), null, 1);

        add("starosta1", DocType.ID_CARD, "041112223", "МВД РК", d, LocalDate.of(2032, 3, 14), 1);
        add("starosta1", DocType.DRIVER_LICENSE, "DL-556677", "МВД РК", LocalDate.of(2025, 5, 20), LocalDate.of(2035, 5, 19), 1);
        add("starosta1", DocType.BIRTH_CERTIFICATE, "BC-112233", "ЗАГС", LocalDate.of(2007, 8, 11), null, 1);

        add("starosta2", DocType.ID_CARD, "041445566", "МВД РК", d, LocalDate.of(2032, 3, 14), 1);
        add("starosta2", DocType.BIRTH_CERTIFICATE, "BC-445566", "ЗАГС", LocalDate.of(2007, 1, 9), null, 1);

        add("teacher1", DocType.ID_CARD, "031000111", "МВД РК", LocalDate.of(2021, 2, 1), LocalDate.of(2031, 1, 31), 1);
        add("teacher1", DocType.DRIVER_LICENSE, "DL-100200", "МВД РК", LocalDate.of(2019, 7, 7), LocalDate.of(2029, 7, 6), 1);
        add("teacher1", DocType.FOREIGN_PASSPORT, "N88001122", "МВД РК", LocalDate.of(2020, 3, 3), LocalDate.of(2030, 3, 2), 1);

        add("teacher2", DocType.ID_CARD, "031000222", "МВД РК", LocalDate.of(2021, 2, 1), LocalDate.of(2031, 1, 31), 1);
        add("parent1", DocType.ID_CARD, "031000333", "МВД РК", LocalDate.of(2021, 2, 1), LocalDate.of(2031, 1, 31), 1);
        add("admin", DocType.ID_CARD, "031000444", "МВД РК", LocalDate.of(2021, 2, 1), LocalDate.of(2031, 1, 31), 1);
    }
}