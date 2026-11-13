package com.redclubes.backend.netpagos;

import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.gestion.*;
import com.redclubes.backend.portal.*;
import com.redclubes.backend.usuarios.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NetPagosSettlementGateTests {
    @Test void verifiedFinalizationDoesNotCreditWhenSettlementIsDisabled() {
        var env = new MockEnvironment(); env.setActiveProfiles("test", "netpagos-sandbox");
        var config = new NetPagosConfig(NetPagosConfig.Mode.SIMULATED, "", 1, false, false, false, 0L, env);
        var intents = mock(NetPaymentIntentRepository.class); var items = mock(NetPaymentItemRepository.class);
        var tokens = new SessionTokenService(); var gateway = mock(NetPagosGateway.class);
        var cobranza = mock(CobranzaService.class); var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        var intent = new NetPaymentIntent(); intent.id = UUID.randomUUID().toString(); intent.clubId = 1L; intent.socioId = 2L;
        intent.providerUuid = UUID.randomUUID().toString(); intent.providerMode = "SIMULATED";
        intent.state = NetPaymentIntent.State.READY; intent.total = new BigDecimal("100.00"); intent.payerHash = tokens.hash("payer:12345678");
        var item = new NetPaymentItem(); item.intentId = intent.id; item.cuotaId = 3L; item.amount = intent.total; item.barcode = "123"; item.service = "TEST";
        when(intents.lock(intent.id)).thenReturn(Optional.of(intent)); when(intents.findById(intent.id)).thenReturn(Optional.of(intent));
        when(items.findByIntentIdOrderByCuotaIdAsc(intent.id)).thenReturn(List.of(item));
        when(gateway.status(intent.providerUuid)).thenReturn(new NetPagosGateway.Status(intent.providerUuid, "Finalizado", "100.00", "12345678",
                List.of(new NetPagosGateway.Item("100.00", "123", "TEST", null))));
        var service = new NetPagosService(config, intents, items, mock(NetPaymentBarcodeRepository.class), mock(NetPaymentReservationRepository.class),
                mock(NetWebhookEventRepository.class), mock(PortalSocioRepository.class), mock(CuotaRepository.class), cobranza, tokens, gateway,
                Clock.systemUTC(), mock(AuthService.class), mock(UsuarioRepository.class), mock(AuditoriaService.class), manager);
        var result = service.refresh(new PortalService.Principal(2L, 1L, ""), intent.id);
        assertEquals(NetPaymentIntent.State.CONFIRMED, result.estado()); assertEquals("SETTLEMENT_DISABLED", result.incidencia());
        verifyNoInteractions(cobranza);
    }
}
