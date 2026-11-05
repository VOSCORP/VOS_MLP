package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.gov.GovDocument;
import com.voscorp.vosmlp.gov.GovDocumentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(9)
public class DocumentSeed implements CommandLineRunner {
    private final UserDocumentRepository docs;
    private final GovDocumentRepository gov;
    private final AppUserRepository users;

    public DocumentSeed(UserDocumentRepository docs, GovDocumentRepository gov, AppUserRepository users) {
        this.docs = docs;
        this.gov = gov;
        this.users = users;
    }

    private void important(String username, DocType type, String adminName) {
        AppUser owner = users.findByUsername(username).orElse(null);
        if (owner == null) { return; }
        byte[] pdf = PdfFactory.simple(type.name().replace('_', ' '), List.of(
                "Holder: " + PdfFactory.translit(owner.getFullName()),
                "Attached by college administration",
                "Date: " + LocalDate.now()));
        docs.save(new UserDocument(owner, type, null, null, null, null, null, null, 1, DocSource.ADMIN,
                type.name().toLowerCase().replace('_', '-') + ".pdf", pdf, adminName));
    }

    @Override
    public void run(String... args) {
        if (docs.count() > 0) { return; }

        for (GovDocument g : gov.findAll()) {
            AppUser owner = g.getOwner();
            // у старосты права есть в VOS Gov, но не импортированы: покажем кнопку «Проверить в VOS Gov»
            if ("starosta1".equals(owner.getUsername()) && g.getType() == DocType.DRIVER_LICENSE) { continue; }

            if ("student1".equals(owner.getUsername()) && g.getType() == DocType.FOREIGN_PASSPORT) {
                // старая версия: срок скоро истекает, в VOS Gov уже есть новая
                byte[] oldPdf = PdfFactory.simple("FOREIGN PASSPORT", List.of(
                        "Holder: " + PdfFactory.translit(owner.getFullName()),
                        "Number: " + g.getNumber(),
                        "Valid until: 2026-12-01",
                        "VOS Gov version: 1"));
                docs.save(new UserDocument(owner, g.getType(), null, null, g.getNumber(), g.getIssuedBy(),
                        LocalDate.of(2019, 6, 1), LocalDate.of(2026, 12, 1), 1, DocSource.GOV,
                        "foreign-passport-v1.pdf", oldPdf, "VOS Gov"));
                continue;
            }
            docs.save(new UserDocument(owner, g.getType(), null, null, g.getNumber(), g.getIssuedBy(),
                    g.getIssuedDate(), g.getExpiryDate(), g.getVersion(), DocSource.GOV,
                    g.getFileName(), g.getContent(), "VOS Gov"));
        }

        // важные документы от администрации (банковских у студентов пока нет)
        for (String username : new String[]{"student1", "starosta1"}) {
            important(username, DocType.COLLEGE_CONTRACT, "Администратор системы");
            important(username, DocType.SCHOOL_CERTIFICATE, "Администратор системы");
        }

        // «Другой документ» студента
        AppUser student1 = users.findByUsername("student1").orElse(null);
        if (student1 != null) {
            byte[] pdf = PdfFactory.simple("RESIDENCE CERTIFICATE", List.of(
                    "Holder: " + PdfFactory.translit(student1.getFullName()),
                    "Uploaded by the user"));
            docs.save(new UserDocument(student1, DocType.OTHER, "Справка с места жительства", "Для заселения в общежитие",
                    null, null, null, null, 1, DocSource.UPLOAD, "residence.pdf", pdf, student1.getFullName()));
        }
    }
}