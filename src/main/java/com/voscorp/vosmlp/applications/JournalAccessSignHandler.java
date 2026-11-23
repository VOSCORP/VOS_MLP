package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.signatures.SignAction;
import com.voscorp.vosmlp.signatures.SignatureHandler;
import com.voscorp.vosmlp.signatures.SignatureRequest;
import org.springframework.stereotype.Component;

@Component
public class JournalAccessSignHandler implements SignatureHandler {
    private final ApplicationRepository apps;
    private final NotificationService notifications;
    private final AuditService audit;

    public JournalAccessSignHandler(ApplicationRepository apps, NotificationService notifications, AuditService audit) {
        this.apps = apps;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Override
    public SignAction action() { return SignAction.JOURNAL_ACCESS; }

    @Override
    public void onConfirmed(SignatureRequest request, AppUser signer) {
        Application a = apps.findById(request.getTargetId()).orElse(null);
        if (a == null || a.getStatus() != ApplicationStatus.PENDING_SIGNATURE) { return; }
        a.markSigned(request.getTraceNumber());
        apps.save(a);

        audit.log(signer, AuditAction.APPLICATION_SUBMITTED,
                "Заявка №" + a.getId() + " · " + signer.getFullName(), null, "на рассмотрении",
                "Номер подписи: " + request.getTraceNumber());
        notifications.send(signer, "Действие подписано через VOS Gov. Заявка №" + a.getId()
                        + " отправлена преподавателю " + a.getRecipient().getFullName() + ", ждите решения.",
                "/applications/" + a.getId());
        notifications.send(a.getRecipient(), "Новая заявка №" + a.getId() + " от старосты " + signer.getFullName()
                + ": доступ к журналу. Откройте и примите решение.", "/applications/" + a.getId());
    }
}