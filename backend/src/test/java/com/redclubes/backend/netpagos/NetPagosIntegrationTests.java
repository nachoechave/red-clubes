package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redclubes.backend.clubes.*;
import com.redclubes.backend.gestion.*;
import com.redclubes.backend.portal.*;
import com.redclubes.backend.socios.*;
import com.redclubes.backend.usuarios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static com.redclubes.backend.netpagos.NetPaymentIntent.State.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:netpagos;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "portal.enabled=true", "portal.club-id=1", "portal.secret=fictional-netpagos-test-secret-32-characters",
        "netpagos.mode=SIMULATED", "netpagos.webhook-enabled=true", "netpagos.settlement-enabled=true", "netpagos.actor-id=999"})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "netpagos-sandbox"})
@Import(NetPagosIntegrationTests.TimeConfig.class)
class NetPagosIntegrationTests {
    static class TestClock extends Clock {
        final AtomicReference<Instant> time = new AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));
        void advance(long seconds) { time.updateAndGet(t -> t.plusSeconds(seconds)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(time.get(), zone); }
        @Override public Instant instant() { return time.get(); }
    }
    @TestConfiguration static class TimeConfig { @Bean @Primary TestClock testClock() { return new TestClock(); } }
    @Autowired TestClock clock;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClubRepository clubs;
    @Autowired SocioRepository socios;
    @Autowired CuotaRepository cuotas;
    @Autowired PagoRepository pagos;
    @Autowired UsuarioRepository users;
    @Autowired SesionUsuarioRepository adminSessions;
    @Autowired SessionTokenService tokens;
    @Autowired PortalService portal;
    @Autowired NetPagosService net;
    @Autowired NetPaymentIntentRepository intents;
    @Autowired NetPaymentItemRepository items;
    @Autowired NetPaymentReservationRepository reserves;
    @Autowired NetPaymentBarcodeRepository barcodes;
    @Autowired NetWebhookEventRepository events;
    @MockitoBean PortalMailService mail;
    @MockitoSpyBean NetPagosGateway gateway;
    String token, header = "Bearer administrator-test-token";
    PortalService.Principal principal;
    Socio ana, other;
    Cuota first, second, foreign, otherInstitution;
    Club bomberos;
    @BeforeEach void fixtures() {
        for (String table : List.of("net_webhook_event", "net_payment_reservation", "net_payment_item", "net_payment_barcode", "net_payment_intent",
                "portal_session", "portal_challenge", "portal_verified_email", "solicitud_inscripcion", "pago", "cuota",
                "auditoria", "socio", "sesion_usuario", "usuario_club", "usuario", "club_modulo", "club")) jdbc.update("delete from " + table);
        jdbc.execute("alter table club alter column id restart with 1");
        jdbc.execute("alter table usuario alter column id restart with 999");
        clock.advance(86400);
        bomberos = clubs.saveAndFlush(new Club("Bomberos ficticios", "Calle de ensayo", EstadoClub.ACTIVO));
        var another = clubs.saveAndFlush(new Club("Otra institucion ficticia", "Otra calle", EstadoClub.ACTIVO));
        ana = member(bomberos, "12345678"); other = member(bomberos, "23456789");
        var otherMember = member(another, "12345678");
        first = cuota(ana, "2026-01", "200.10"); second = cuota(ana, "2026-02", "300.20");
        foreign = cuota(other, "2026-01", "700.00"); otherInstitution = cuota(otherMember, "2026-01", "800.00");
        var admin = users.saveAndFlush(new Usuario("99887766", "Admin", "Ficticio", RolUsuario.SUPERUSUARIO, EstadoUsuario.ACTIVO, "unused", false));
        assertEquals(999, admin.getId());
        adminSessions.saveAndFlush(new SesionUsuario(tokens.hash("administrator-test-token"), LocalDateTime.now().plusHours(2), admin));
        var code = new AtomicReference<String>();
        doAnswer(i -> { code.set(i.getArgument(1)); return null; }).when(mail).enqueue(anyString(), anyString());
        portal.activate(header, ana.getId(), "Acta ficticia", false);
        var request = portal.requestCode(ana.getDni(), "127.0.0.1");
        token = portal.verify(request.solicitudId(), code.get(), "127.0.0.1").token();
        principal = portal.authenticate("Bearer " + token);
        register(first); register(second);
    }
    Socio member(Club club, String dni) {
        var socio = new Socio(null, "Ana", "Ficticia", dni, "ACTIVO"); socio.setClub(club); socio.setEmail("test@example.invalid");
        return socios.saveAndFlush(socio);
    }
    Cuota cuota(Socio socio, String period, String amount) {
        var c = new Cuota(); c.setClub(socio.getClub()); c.setSocio(socio); c.setPeriodo(period);
        c.setImporte(new BigDecimal(amount)); c.setEstado(EstadoCuota.PENDIENTE); c.setVencimiento(LocalDate.now(clock).plusDays(10));
        return cuotas.saveAndFlush(c);
    }
    void register(Cuota c) { net.registerBarcode(header, c.getId(), "900000000000" + c.getId(), "BOMBEROS_TEST", LocalDate.now(clock).plusDays(10), "Convenio ficticio"); }
    NetPagosService.View create(Long... ids) { return net.create(principal, List.of(ids), UUID.randomUUID().toString(), "1"); }
    NetPaymentIntent stored(NetPagosService.View view) { return intents.findById(view.id()).orElseThrow(); }
    NetPagosService.View finish(NetPagosService.View view, String state, List<Boolean> paid) {
        net.simulate(header, view.id(), state, paid); clock.advance(31); return net.refresh(principal, view.id());
    }
    @Test void createsSnapshotAndUsesServerBalanceAndLockedPayer() {
        var order = create(first.getId(), second.getId()); assertEquals(READY, order.estado());
        assertEquals(new BigDecimal("500.30"), order.total()); assertTrue(order.simulado()); assertEquals(2, reserves.count());
        var capture = org.mockito.ArgumentCaptor.forClass(NetPagosGateway.Create.class); verify(gateway).create(capture.capture());
        assertTrue(capture.getValue().payer().lockedPayer()); assertEquals("1", capture.getValue().payer().documentType());
        assertEquals("12345678", capture.getValue().payer().documentNumber());
        assertEquals(List.of("200.10", "300.20"), capture.getValue().payments().stream().map(NetPagosGateway.Payment::amount).toList());
        assertEquals(0, pagos.count());
    }
    @Test void sameKeyIsIdempotentAndChangedSelectionConflicts() {
        String key = UUID.randomUUID().toString();
        var a = net.create(principal, List.of(first.getId()), key, "1");
        var b = net.create(principal, List.of(first.getId()), key, "1"); assertEquals(a.id(), b.id());
        assertThrows(ResponseStatusException.class, () -> net.create(principal, List.of(second.getId()), key, "1"));
        verify(gateway, times(1)).create(any()); assertEquals(1, intents.count());
    }
    @Test void concurrentSameKeyPostsOnlyOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        try (var pool = Executors.newFixedThreadPool(6)) {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 6; i++) calls.add(() -> net.create(principal, List.of(first.getId()), key, "1").id());
            var ids = new HashSet<String>(); for (var r : pool.invokeAll(calls)) ids.add(r.get()); assertEquals(1, ids.size());
        }
        verify(gateway, times(1)).create(any()); assertEquals(1, intents.count());
    }
    @Test void refusesForeignQuotasAndDuplicateSelections() {
        for (Long id : List.of(foreign.getId(), otherInstitution.getId())) assertThrows(ResponseStatusException.class, () -> create(id));
        assertThrows(ResponseStatusException.class, () -> net.create(principal, List.of(first.getId(), first.getId()), UUID.randomUUID().toString(), "1"));
        assertEquals(0, intents.count()); verify(gateway, never()).create(any());
    }
    @Test void refusesUnconfiguredExpiredOrStaleBarcodesAndAlreadyPaidQuotas() {
        barcodes.deleteById(first.getId()); assertThrows(ResponseStatusException.class, () -> create(first.getId())); register(first);
        var row = barcodes.findById(first.getId()).orElseThrow(); row.validUntil = LocalDate.now(clock).minusDays(1); barcodes.saveAndFlush(row);
        assertThrows(ResponseStatusException.class, () -> create(first.getId())); register(first);
        first.setImporte(new BigDecimal("201.10")); cuotas.saveAndFlush(first);
        assertThrows(ResponseStatusException.class, () -> create(first.getId()));
        second.setEstado(EstadoCuota.PAGADA); cuotas.saveAndFlush(second);
        assertThrows(ResponseStatusException.class, () -> create(second.getId())); assertEquals(0, intents.count());
    }
    @Test void reservationsPreventSecondOrderAndAdministrativeDoubleCollection() throws Exception {
        create(first.getId()); assertThrows(ResponseStatusException.class, () -> create(first.getId()));
        mvc.perform(post("/api/clubes/1/cuotas/" + first.getId() + "/pagos").header("Authorization", header)
                .contentType(APPLICATION_JSON).content("{\"importe\":200.10,\"medioPago\":\"EFECTIVO\"}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, pagos.count());
    }
    @Test void lostCreateResponseDoesNotRetryAndKeepsReservation() {
        doThrow(new NetPagosGateway.Failure()).when(gateway).create(any());
        String key = UUID.randomUUID().toString(); var a = net.create(principal, List.of(first.getId()), key, "1");
        assertEquals(UNKNOWN, a.estado()); net.create(principal, List.of(first.getId()), key, "1");
        assertThrows(ResponseStatusException.class, () -> create(first.getId()));
        verify(gateway, times(1)).create(any()); assertEquals(1, reserves.count());
    }
    @Test void finalizationRegistersEachPaymentExactlyOnceAndAuditsIt() {
        var order = create(first.getId(), second.getId()); var done = finish(order, "Finalizado", List.of(true, true));
        assertEquals(APPLIED, done.estado()); assertEquals(2, pagos.count()); assertEquals(0, reserves.count());
        assertTrue(done.cuotas().stream().allMatch(NetPagosService.ItemView::acreditado));
        clock.advance(31); net.refresh(principal, order.id()); assertEquals(2, pagos.count());
        assertTrue(pagos.findAll().stream().allMatch(p -> p.getMedioPago() == MedioPago.NET_PAGOS && p.getReferenciaExterna() != null));
        assertEquals(2, jdbc.queryForObject("select count(*) from auditoria where tipo_entidad='PAGO'", Integer.class));
    }
    @Test void partialCartOnlyCreditsVerifiedPaidItems() {
        var order = create(first.getId(), second.getId()); var done = finish(order, "Parcial", List.of(true, false));
        assertEquals(APPLIED, done.estado()); assertEquals(1, pagos.count());
        assertEquals(first.getId(), pagos.findAll().getFirst().getCuota().getId());
        assertEquals(EstadoCuota.PENDIENTE, cuotas.findById(second.getId()).orElseThrow().getEstado());
        assertEquals(0, reserves.count()); create(second.getId());
    }
    @Test void restPartialWithoutItemTruthRequiresReview() {
        var order = create(first.getId(), second.getId()); String uuid = stored(order).providerUuid;
        var initial = gateway.status(uuid);
        doReturn(new NetPagosGateway.Status(uuid, "Parcial", initial.totalAmount(), initial.documentNumber(), initial.payments())).when(gateway).status(uuid);
        assertEquals(REVIEW, net.refresh(principal, order.id()).estado()); assertEquals(0, pagos.count()); assertEquals(2, reserves.count());
    }
    @Test void badAmountsPayerOrItemsAreNeverCredited() {
        var order = create(first.getId()); var original = gateway.status(stored(order).providerUuid);
        doReturn(new NetPagosGateway.Status(original.uuid(), "Finalizado", "999.99", original.documentNumber(), original.payments())).when(gateway).status(original.uuid());
        assertEquals(REVIEW, net.refresh(principal, order.id()).estado()); assertEquals(0, pagos.count());
        doReturn(new NetPagosGateway.Status(original.uuid(), "Finalizado", original.totalAmount(), "99999999", original.payments())).when(gateway).status(original.uuid());
        clock.advance(31); assertEquals(REVIEW, net.refresh(principal, order.id()).estado()); assertEquals(0, pagos.count());
        doReturn(new NetPagosGateway.Status(original.uuid(), "Finalizado", original.totalAmount(), original.documentNumber(),
                List.of(new NetPagosGateway.Item("200.10", "999", "FAKE", true)))).when(gateway).status(original.uuid());
        clock.advance(31); assertEquals(REVIEW, net.refresh(principal, order.id()).estado()); assertEquals(1, reserves.count());
    }
    @Test void changedLocalBalanceStopsEntireCartBeforeAnyCredit() {
        var order = create(first.getId(), second.getId()); second.setImporte(new BigDecimal("301.20")); cuotas.saveAndFlush(second);
        assertEquals(REVIEW, finish(order, "Finalizado", List.of(true, true)).estado()); assertEquals(0, pagos.count());
    }
    @Test void cancellationReleasesReservationsWithoutRecordingPayments() {
        var order = create(first.getId()); assertEquals(CANCELLED, finish(order, "Cancelado", List.of(false)).estado());
        assertEquals(0, pagos.count()); assertEquals(0, reserves.count()); create(first.getId());
    }
    @Test void failedQueriesKeepReservationsAndDoNotFabricateCancellation() {
        var order = create(first.getId()); doThrow(new NetPagosGateway.Failure()).when(gateway).status(anyString());
        assertEquals(READY, net.refresh(principal, order.id()).estado()); assertEquals(1, reserves.count()); assertEquals(0, pagos.count());
    }
    @Test void reconciliationIsRateLimitedAndConcurrentCreditsAreIdempotent() throws Exception {
        var order = create(first.getId()); net.simulate(header, order.id(), "Finalizado", List.of(true));
        clearInvocations(gateway); // The simulator's state transition reads its own record internally.
        try (var pool = Executors.newFixedThreadPool(5)) {
            List<Callable<NetPagosService.View>> calls = new ArrayList<>();
            for (int i = 0; i < 5; i++) calls.add(() -> net.refresh(principal, order.id()));
            for (var r : pool.invokeAll(calls)) r.get();
        }
        assertEquals(1, pagos.count()); verify(gateway, times(1)).status(stored(order).providerUuid);
    }
    @Test void webhookIsUntrustedDeduplicatedAndRetainsNoPayerData() throws Exception {
        var order = create(first.getId()); String uuid = stored(order).providerUuid;
        String body = json.writeValueAsString(Map.of("status", "Finalizado", "preorder", Map.of("uuid", uuid,
                "payer", Map.of("email", "private@example.test", "document_number", "12345678"))));
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/integraciones/netpagos/webhook").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted());
        assertEquals(1, events.count()); assertEquals(READY, stored(order).state); assertEquals(0, pagos.count());
        assertEquals(1, reserves.count());
        // The simulator's authenticated pull still says Instanciado, regardless of the forged callback.
        assertEquals(READY, net.refresh(principal, order.id()).estado());
    }
    @Test void unknownWebhookDoesNotCreateOrphanRowsAndOversizeIsRejected() throws Exception {
        mvc.perform(post("/api/integraciones/netpagos/webhook").contentType(APPLICATION_JSON)
                .content("{\"status\":\"Finalizado\",\"preorder\":{\"uuid\":\"" + UUID.randomUUID() + "\"}}"))
                .andExpect(status().isAccepted()); assertEquals(0, events.count());
        mvc.perform(post("/api/integraciones/netpagos/webhook").contentType(APPLICATION_JSON).content(" ".repeat(16385)))
                .andExpect(status().isPayloadTooLarge());
    }
    @Test void browserCannotSupplyAmountAndPortalCannotUseAdministrativeEndpoints() throws Exception {
        mvc.perform(post("/api/portal-bomberos/pagos").header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("cuotaIds", List.of(first.getId()), "claveSolicitud", UUID.randomUUID(), "generoDocumento", "1", "importe", 1))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/netpagos-administracion/cuotas/" + first.getId() + "/barra").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/portal-bomberos/pagos").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()); assertEquals(0, intents.count());
    }
    @Test void intentCannotBeReadByAnotherMemberOrInstitution() throws Exception {
        var order = create(first.getId());
        assertThrows(ResponseStatusException.class, () -> net.view(new PortalService.Principal(other.getId(), 1L, ""), order.id()));
        assertThrows(ResponseStatusException.class, () -> net.view(new PortalService.Principal(ana.getId(), 2L, ""), order.id()));
        mvc.perform(get("/api/portal-bomberos/pagos/" + order.id()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(200.10)).andExpect(jsonPath("$.payer").doesNotExist());
    }
    @Test void recoverAfterLostResponseRequiresVerifiedMatchingOrder() {
        var realCreate = new AtomicReference<NetPagosGateway.Created>();
        doAnswer(invocation -> { var result = (NetPagosGateway.Created) invocation.callRealMethod(); realCreate.set(result); throw new NetPagosGateway.Failure(); })
                .when(gateway).create(any());
        var unknown = create(first.getId()); assertEquals(UNKNOWN, unknown.estado());
        var recovered = net.recover(header, unknown.id(), realCreate.get().uuid()); assertEquals(READY, recovered.estado());
        assertEquals(realCreate.get().uuid(), stored(recovered).providerUuid); verify(gateway, times(1)).create(any());
    }
    @Test void manualApiCannotPretendToBeNetPagos() throws Exception {
        mvc.perform(post("/api/clubes/1/cuotas/" + first.getId() + "/pagos").header("Authorization", header)
                .contentType(APPLICATION_JSON).content("{\"importe\":200.10,\"medioPago\":\"NET_PAGOS\"}"))
                .andExpect(status().isBadRequest()); assertEquals(0, pagos.count());
    }
    @Test void backgroundReconciliationDoesNotNeedTheBrowserToReturn() {
        var order = create(first.getId()); net.simulate(header, order.id(), "Finalizado", List.of(true));
        net.reconcilePending(); assertEquals(APPLIED, stored(order).state); assertEquals(1, pagos.count());
        net.reconcilePending(); assertEquals(1, pagos.count());
    }
    @Test void cartLimitAndReviewQueueAreEnforced() throws Exception {
        assertThrows(ResponseStatusException.class, () -> net.create(principal, java.util.stream.LongStream.rangeClosed(1, 11).boxed().toList(), UUID.randomUUID().toString(), "1"));
        doThrow(new NetPagosGateway.Failure()).when(gateway).create(any()); create(first.getId());
        mvc.perform(get("/api/netpagos-administracion/intenciones").param("estado", "UNKNOWN").header("Authorization", header))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/netpagos-administracion/intenciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
    @Test void supportsTheDocumentedMaximumOfTenItems() {
        var selected = new ArrayList<>(List.of(first.getId(), second.getId()));
        for (int n = 3; n <= 10; n++) { var c = cuota(ana, "2026-" + String.format("%02d", n), "10.00"); register(c); selected.add(c.getId()); }
        var order = net.create(principal, selected, UUID.randomUUID().toString(), "3");
        assertEquals(10, order.cuotas().size()); assertEquals(new BigDecimal("580.30"), order.total()); assertEquals(10, reserves.count());
    }
    @Test void lateChangesToPaidItemsNeverCreateAdditionalPayments() {
        var order = create(first.getId(), second.getId()); finish(order, "Parcial", List.of(true, false));
        var late = finish(order, "Finalizado", List.of(true, true));
        assertEquals(APPLIED, late.estado()); assertEquals("LATE_ITEM_RESULT_REQUIRES_REVIEW", late.incidencia()); assertEquals(1, pagos.count());
    }
    @Test void administrativelyAnnulledExternalPaymentIsNotCreatedAgain() throws Exception {
        var order = create(first.getId()); finish(order, "Finalizado", List.of(true));
        Long pagoId = pagos.findAll().getFirst().getId();
        mvc.perform(post("/api/clubes/1/cuotas/" + first.getId() + "/pagos/" + pagoId + "/anulacion").header("Authorization", header)
                .contentType(APPLICATION_JSON).content("{\"motivo\":\"Ensayo ficticio de anulacion\"}"))
                .andExpect(status().isOk());
        clock.advance(31); net.refresh(principal, order.id());
        assertEquals(1, pagos.count()); assertEquals(EstadoPago.ANULADO, pagos.findById(pagoId).orElseThrow().getEstado());
    }
    @Test void recoveryCannotCrossProviderEnvironments() {
        doThrow(new NetPagosGateway.Failure()).when(gateway).create(any());
        var order = create(first.getId());
        jdbc.update("update net_payment_intent set provider_mode='STAGING' where id=?", order.id());
        assertThrows(ResponseStatusException.class, () -> net.recover(header, order.id(), UUID.randomUUID().toString()));
        verify(gateway, never()).status(anyString()); assertEquals(0, pagos.count());
    }
}
