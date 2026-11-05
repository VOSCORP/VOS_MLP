package com.voscorp.vosmlp.documents;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.gov.GovDocument;
import com.voscorp.vosmlp.gov.GovDocumentRepository;
import com.voscorp.vosmlp.notifications.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DocumentService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserDocumentRepository docs;
    private final GovDocumentRepository gov;
    private final AppUserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;

    public DocumentService(UserDocumentRepository docs, GovDocumentRepository gov, AppUserRepository users,
                           AuditService audit, NotificationService notifications) {
        this.docs = docs;
        this.gov = gov;
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
    }

    // ---------- показ ----------

    private UserDocument first(AppUser owner, DocType type) {
        return docs.findByOwnerAndType(owner, type).stream().findFirst().orElse(null);
    }

    private DocView view(UserDocument d) {
        DocType type = d.getType();
        String title = (type == DocType.OTHER && d.getTitle() != null) ? d.getTitle() : type.getLabel();
        String issued = d.getIssuedDate() == null ? "" : d.getIssuedDate().format(DATE);
        String uploaded = d.getUploadedAt().format(DATE);
        String expiry = "";
        String status;
        String state = "ok";
        String note = "";

        if (type.isImportant()) {
            status = "Прикреплён " + uploaded;
            note = "Прикрепил(а): " + d.getUploadedBy();
        } else if (type == DocType.OTHER) {
            status = "Загружен " + uploaded;
            note = "Для чего: " + (d.getPurpose() == null ? "" : d.getPurpose());
        } else if (d.getExpiryDate() == null) {
            expiry = "Бессрочный";
            status = "Бессрочный";
        } else {
            LocalDate today = LocalDate.now();
            expiry = d.getExpiryDate().format(DATE);
            if (d.getExpiryDate().isBefore(today)) {
                status = "Срок истёк " + expiry;
                state = "bad";
            } else if (d.getExpiryDate().isBefore(today.plusDays(60))) {
                status = "Истекает " + expiry;
                state = "warn";
            } else {
                status = "Действителен до " + expiry;
            }
        }
        String source = d.getSource() == DocSource.GOV
                ? "VOS Gov, версия " + d.getVersion() : d.getSource().getLabel();
        return new DocView(d.getId(), type.getLabel(), title, d.getNumber() == null ? "" : d.getNumber(),
                d.getIssuedBy() == null ? "" : d.getIssuedBy(), issued, expiry, status, state, source, note,
                d.getFileName(), type.isGovType(), type == DocType.OTHER);
    }

    private List<DocSlot> slots(AppUser user, List<DocType> types) {
        List<DocSlot> result = new ArrayList<>();
        for (DocType t : types) {
            UserDocument d = first(user, t);
            result.add(new DocSlot(t.name(), t.getLabel(), t.getMissingText(), t.isGovType(),
                    d == null ? null : view(d)));
        }
        return result;
    }

    public List<DocSlot> govSlots(AppUser user) { return slots(user, DocType.govTypes()); }

    public List<DocSlot> importantSlots(AppUser user) { return slots(user, DocType.importantTypes()); }

    public List<DocView> otherDocs(AppUser user) {
        List<DocView> result = new ArrayList<>();
        for (UserDocument d : docs.findByOwnerAndType(user, DocType.OTHER)) { result.add(view(d)); }
        result.sort(Comparator.comparing(DocView::getId).reversed());
        return result;
    }

    // ---------- загрузка ----------

    private String checkPdf(String originalName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) { return "Выберите файл"; }
        if (bytes.length > MAX_SIZE) { return "Файл больше 5 МБ"; }
        String name = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (!name.endsWith(".pdf")) { return "Можно загружать только файлы PDF"; }
        String head = new String(bytes, 0, Math.min(5, bytes.length), StandardCharsets.US_ASCII);
        if (!head.startsWith("%PDF")) { return "Файл не похож на PDF"; }
        return null;
    }

    private byte[] read(MultipartFile file) {
        try {
            return file == null ? null : file.getBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) { return "document.pdf"; }
        return name.replaceAll("[\\\\/]", "_");
    }

    // Другой документ (любая роль)
    public String uploadOther(AppUser user, String title, String purpose, MultipartFile file) {
        if (title == null || title.isBlank()) { return "Напишите, что это за документ"; }
        if (purpose == null || purpose.isBlank()) { return "Напишите, для чего этот документ"; }
        if (title.length() > 150 || purpose.length() > 300) { return "Описание слишком длинное"; }
        byte[] bytes = read(file);
        String error = checkPdf(file == null ? null : file.getOriginalFilename(), bytes);
        if (error != null) { return error; }

        UserDocument d = docs.save(new UserDocument(user, DocType.OTHER, title.trim(), purpose.trim(), null, null,
                null, null, 1, DocSource.UPLOAD, safeName(file.getOriginalFilename()), bytes, user.getFullName()));
        audit.log(user, AuditAction.DOCUMENT_UPLOADED, "Документ «" + d.getTitle() + "»", null, d.getFileName(), null);
        return null;
    }

    // Важный документ (только админ)
    public String uploadImportant(AppUser admin, Long ownerId, String typeRaw, MultipartFile file) {
        if (admin.getRole() != UserRole.ADMIN) { return "Прикреплять важные документы могут только администраторы"; }
        AppUser owner = ownerId == null ? null : users.findById(ownerId).orElse(null);
        if (owner == null) { return "Выберите студента"; }
        DocType type;
        try {
            type = DocType.valueOf(typeRaw);
        } catch (Exception e) {
            return "Выберите тип документа";
        }
        if (!type.isImportant()) { return "Выберите один из четырёх важных документов"; }
        byte[] bytes = read(file);
        String error = checkPdf(file == null ? null : file.getOriginalFilename(), bytes);
        if (error != null) { return error; }

        // один документ каждого типа: старый заменяется новым
        docs.deleteAll(docs.findByOwnerAndType(owner, type));
        docs.save(new UserDocument(owner, type, null, null, null, null, null, null, 1, DocSource.ADMIN,
                safeName(file.getOriginalFilename()), bytes, admin.getFullName()));
        audit.log(admin, AuditAction.DOCUMENT_UPLOADED, owner.getFullName() + " · " + type.getLabel(),
                null, file.getOriginalFilename(), "Важный документ");
        notifications.send(owner, "Администрация прикрепила документ «" + type.getLabel() + "».",
                "/documents?tab=important");
        return null;
    }

    // ---------- сверка с VOS Gov (заглушка) ----------

    // Возвращает сообщение для пользователя
    public String syncFromGov(AppUser user, DocType type) {
        if (!type.isGovType()) { return "Этот документ не берётся из VOS Gov"; }
        GovDocument g = gov.findByOwnerAndType(user, type).orElse(null);
        if (g == null) { return "В VOS Gov нет этого документа"; }
        UserDocument d = first(user, type);
        if (d == null) {
            docs.save(new UserDocument(user, type, null, null, g.getNumber(), g.getIssuedBy(), g.getIssuedDate(),
                    g.getExpiryDate(), g.getVersion(), DocSource.GOV, g.getFileName(), g.getContent(), "VOS Gov"));
            audit.log(user, AuditAction.DOCUMENT_SYNCED, type.getLabel(), "нет", "добавлен из VOS Gov", null);
            return "Документ «" + type.getLabel() + "» добавлен из VOS Gov.";
        }
        if (g.getVersion() > d.getVersion()) {
            int old = d.getVersion();
            d.updateFromGov(g);
            docs.save(d);
            audit.log(user, AuditAction.DOCUMENT_SYNCED, type.getLabel(), "версия " + old,
                    "версия " + g.getVersion(), "Обновлено из VOS Gov");
            return "Документ обновлён: в VOS Gov появилась новая версия.";
        }
        return "Изменений нет: в VOS Gov документ не обновлялся, поэтому изменить его нельзя.";
    }

    public String syncExisting(AppUser user, Long id) {
        UserDocument d = id == null ? null : docs.findById(id).orElse(null);
        if (d == null || !d.getOwner().getId().equals(user.getId())) { return "Документ не найден"; }
        return syncFromGov(user, d.getType());
    }

    // ---------- скачивание и удаление ----------

    // документ можно скачать владельцу и админу
    public UserDocument forDownload(AppUser user, Long id) {
        UserDocument d = id == null ? null : docs.findById(id).orElse(null);
        if (d == null) { return null; }
        boolean ok = d.getOwner().getId().equals(user.getId()) || user.getRole() == UserRole.ADMIN;
        return ok ? d : null;
    }

    // удалить можно только свой «Другой документ»
    public String deleteOther(AppUser user, Long id) {
        UserDocument d = id == null ? null : docs.findById(id).orElse(null);
        if (d == null || d.getType() != DocType.OTHER || !d.getOwner().getId().equals(user.getId())) {
            return "Этот документ удалить нельзя";
        }
        docs.delete(d);
        audit.log(user, AuditAction.DOCUMENT_REMOVED, "Документ «" + d.getTitle() + "»", d.getFileName(), null, null);
        return null;
    }

    // важные документы удаляет только админ
    public String deleteImportant(AppUser admin, Long id) {
        if (admin.getRole() != UserRole.ADMIN) { return "Удалять важные документы могут только администраторы"; }
        UserDocument d = id == null ? null : docs.findById(id).orElse(null);
        if (d == null || !d.getType().isImportant()) { return "Документ не найден"; }
        docs.delete(d);
        audit.log(admin, AuditAction.DOCUMENT_REMOVED, d.getOwner().getFullName() + " · " + d.getType().getLabel(),
                d.getFileName(), null, "Важный документ");
        return null;
    }

    // ---------- для страницы админа ----------

    public List<AdminDocRow> importantRows() {
        List<AdminDocRow> rows = new ArrayList<>();
        for (UserDocument d : docs.findByTypeIn(DocType.importantTypes())) {
            rows.add(new AdminDocRow(d.getId(), d.getOwner().getFullName(), d.getType().getLabel(),
                    d.getFileName(), d.getUploadedAt().format(DATE), d.getUploadedBy()));
        }
        rows.sort(Comparator.comparing(AdminDocRow::getOwnerName).thenComparing(AdminDocRow::getTypeLabel));
        return rows;
    }

    public List<AppUser> studentAccounts() {
        List<AppUser> list = new ArrayList<>();
        for (AppUser u : users.findAll()) {
            if (u.getRole() == UserRole.STUDENT || u.getRole() == UserRole.STAROSTA) { list.add(u); }
        }
        list.sort(Comparator.comparing(AppUser::getFullName));
        return list;
    }
}