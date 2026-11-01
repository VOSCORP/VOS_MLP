package com.voscorp.vosmlp.audit;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditListener {
    private final AuditService audit;

    public AuthAuditListener(AuditService audit) {
        this.audit = audit;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        audit.login(event.getAuthentication().getName(), true);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        audit.login(event.getAuthentication().getName(), false);
    }
}