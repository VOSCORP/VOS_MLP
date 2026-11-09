package com.voscorp.vosmlp.applications;

import com.voscorp.vosmlp.audit.AuditAction;
import com.voscorp.vosmlp.audit.AuditService;
import com.voscorp.vosmlp.auth.AppUser;
import com.voscorp.vosmlp.auth.AppUserRepository;
import com.voscorp.vosmlp.auth.UserRole;
import com.voscorp.vosmlp.notifications.NotificationService;
import com.voscorp.vosmlp.signatures.SignAction;
import com.voscorp.vosmlp.signatures.SignatureHandler;
import com.voscorp.vosmlp.signatures.SignatureRequest;
import org.springframework.stereotype.Component;

@Component
public class AttendanceChangeSignHandler implements SignatureHandler {
    private final ApplicationRepository apps;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final AuditService audit;

    public AttendanceChangeSignHandler(ApplicationRepository apps, AppUserRepository users,
                                       NotificationService notifications, AuditService audit) {
        this.apps = apps;
        this.users = users;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Override
    public SignAction action() { return SignAction.ATTENDANCE_CHANGE; }

    @Override
    public void onConfirmed(SignatureRequest request, AppUser signer) {
        Application a = apps.findById(request.getTargetId()).orElse(null);
        if (a == null || a.getStatus() != ApplicationStatus.PENDING_SIGNATURE) { return; }
        a.markSigned(request.getTraceNumber());
        apps.save(a);

        audit.log(signer, AuditAction.APPLICATION_SUBMITTED,
                "Заявка №" + a.getId() + " · " + signer.getFullName(), null, "на рассмотрении",
                "Номер подписи: " + request.getTraceNumber());
        // этот toast студент увидит сразу после возврата из VOS Gov
        notifications.send(signer, "Действие подписано через VOS Gov. Заявка №" + a.getId()
                + " отправлена администрации, ждите решения.", "/applications/" + a.getId());
        for (AppUser admin : users.findByRole(UserRole.ADMIN)) {
            notifications.send(admin, "Новая заявка №" + a.getId() + ": изменение посещаемости от "
                    + signer.getFullName() + ". Требуется решение.", "/admin/applications/" + a.getId());
        }
    }
}