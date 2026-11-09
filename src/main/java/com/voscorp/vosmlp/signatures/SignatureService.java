package com.voscorp.vosmlp.signatures;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.gov.GovSignature;
import com.voscorp.vosmlp.gov.GovSignatureRepository;
import com.voscorp.vosmlp.notifications.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SignatureService {
    private final SignatureRequestRepository repo;
    private final GovSignatureRepository govSignatures;
    private final AuditService audit;
    private final NotificationService notifications;
    private final Map<SignAction, SignatureHandler> handlers = new EnumMap<>(SignAction.class);

    public SignatureService(SignatureRequestRepository repo, GovSignatureRepository govSignatures,
                            AuditService audit, NotificationService notifications,
                            List<SignatureHandler> handlerList) {
        this.repo = repo;
        this.govSignatures = govSignatures;
        this.audit = audit;
        this.notifications = notifications;
        for (SignatureHandler h : handlerList) { handlers.put(h.action(), h); }
    }

    // ОДНА СТРОКА для любого действия: создаёт запрос на подпись и отправляет пользователя в VOS Gov
    public String require(AppUser user, SignAction action, Long targetId, String summary, String returnUrl) {
        SignatureRequest r = repo.save(new SignatureRequest(UUID.randomUUID().toString(), action, targetId,
                user, summary, returnUrl));
        return "redirect:/gov/sign/" + r.getToken();
    }

    // запрос на подпись доступен только тому, кто его создал
    public SignatureRequest findFor(AppUser user, String token) {
        SignatureRequest r = repo.findByToken(token).orElse(null);
        return (r != null && r.getRequester().getId().equals(user.getId())) ? r : null;
    }

    public void sign(SignatureRequest r) {
        if (r.getStatus() == SignStatus.CREATED) {
            r.sign();
            repo.save(r);
        }
    }

    public void confirm(SignatureRequest r) {
        if (r.getStatus() != SignStatus.SIGNED) { return; }
        String trace = "GOV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format("%05d", govSignatures.count() + 1);
        govSignatures.save(new GovSignature(trace, r.getRequester().getFullName(),
                r.getAction().getLabel(), r.getSummary(), "VOS College"));
        r.confirm(trace);
        repo.save(r);
        audit.log(r.getRequester(), AuditAction.SIGNED, r.getSummary(), null,
                "подписано через VOS Gov", "Номер подписи: " + trace);

        SignatureHandler handler = handlers.get(r.getAction());
        if (handler != null) { handler.onConfirmed(r, r.getRequester()); }
    }

    public void cancel(SignatureRequest r) {
        if (r.getStatus() == SignStatus.CREATED || r.getStatus() == SignStatus.SIGNED) {
            r.cancel();
            repo.save(r);
            notifications.send(r.getRequester(),
                    "Подпись отменена: действие не выполнено. Подписать можно позже в разделе «Заявки».",
                    r.getReturnUrl());
        }
    }
}