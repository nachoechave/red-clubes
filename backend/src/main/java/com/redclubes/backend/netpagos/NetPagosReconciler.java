package com.redclubes.backend.netpagos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NetPagosReconciler {
    private final NetPagosService service;
    private final boolean enabled;
    NetPagosReconciler(NetPagosService service, @Value("${netpagos.reconciliation-enabled:false}") boolean enabled) {
        this.service = service; this.enabled = enabled;
    }
    @Scheduled(fixedDelayString = "${netpagos.reconcile-delay-ms:60000}")
    public void reconcile() { if (enabled) service.reconcilePending(); }
}
