package com.redclubes.backend.portal;

import com.fasterxml.jackson.databind.*;
import com.redclubes.backend.clubes.*;
import com.redclubes.backend.socios.*;
import com.redclubes.backend.gestion.*;
import com.redclubes.backend.usuarios.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:portal;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "portal.enabled=true", "portal.club-id=1", "portal.secret=fictional-test-only-secret-32-characters",
        "portal.allowed-origins=https://bomberos.example.test"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PortalIntegrationTests.TimeConfig.class)
class PortalIntegrationTests {
    static class TestClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));
        void advance(long seconds) { now.updateAndGet(t -> t.plusSeconds(seconds)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now.get(), zone); }
        @Override public Instant instant() { return now.get(); }
    }
    @TestConfiguration static class TimeConfig {
        @Bean @Primary TestClock testClock() { return new TestClock(); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClubRepository clubs;
    @Autowired SocioRepository socios;
    @Autowired CuotaRepository cuotas;
    @Autowired UsuarioRepository users;
    @Autowired SesionUsuarioRepository adminSessions;
    @Autowired SessionTokenService tokens;
    @Autowired PortalService service;
    @Autowired PortalChallengeRepository challenges;
    @Autowired PortalSessionRepository sessions;
    @Autowired PortalVerifiedEmailRepository emails;
    @Autowired TestClock clock;
    @MockitoBean PortalMailService mail;
    final List<String> delivered = new CopyOnWriteArrayList<>();
    Socio ana, other, sameDniOtherClub;
    Club bomberos, otherClub;
    String adminHeader = "Bearer test-administrator-token";

    @BeforeEach void fixtures() {
        for (String table : List.of("portal_session", "portal_challenge", "portal_verified_email", "pago", "cuota",
                "auditoria", "socio", "sesion_usuario", "usuario_club", "usuario", "club_modulo", "club"))
            jdbc.update("delete from " + table);
        jdbc.execute("alter table club alter column id restart with 1");
        clock.advance(86400);
        delivered.clear();
        doAnswer(invocation -> { delivered.add(invocation.getArgument(1)); return null; }).when(mail).enqueue(anyString(), anyString());
        bomberos = clubs.saveAndFlush(new Club("Bomberos ficticios", "Calle ficticia 123", EstadoClub.ACTIVO));
        otherClub = clubs.saveAndFlush(new Club("Otra institucion", "Otra calle 456", EstadoClub.ACTIVO));
        ana = member(bomberos, "12345678", "Ana");
        other = member(bomberos, "23456789", "Berta");
        sameDniOtherClub = member(otherClub, "12345678", "Carla");
        var admin = users.saveAndFlush(new Usuario("99887766", "Admin", "Prueba", RolUsuario.SUPERUSUARIO,
                EstadoUsuario.ACTIVO, "unused", false));
        adminSessions.saveAndFlush(new SesionUsuario(tokens.hash("test-administrator-token"), LocalDateTime.now().plusHours(2), admin));
    }
    Socio member(Club club, String dni, String name) {
        var socio = new Socio(null, name, "Ficticia", dni, "ACTIVO");
        socio.setClub(club); socio.setEmail(name.toLowerCase(Locale.ROOT) + "@example.test");
        return socios.saveAndFlush(socio);
    }
    void activate(Socio s) { service.activate(adminHeader, s.getId(), "Acta ficticia de verificacion presencial 001", false); }
    String request(String dni) throws Exception {
        String body = mvc.perform(post("/api/portal-bomberos/auth/solicitar-codigo").contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("dni", dni))))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.codigo").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("solicitudId").asText();
    }
    ResultActions verify(String id, String code) throws Exception {
        return mvc.perform(post("/api/portal-bomberos/auth/verificar-codigo").contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("solicitudId", id, "codigo", code))));
    }
    String login() throws Exception {
        activate(ana);
        String id = request(ana.getDni());
        return mapper.readTree(verify(id, delivered.getLast()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asText();
    }
    @Test void nonexistentAndUnverifiedHaveSameContractAndNoDelivery() throws Exception {
        String a = request("00000000");
        String b = request(ana.getDni());
        assertNotEquals(a, b); assertTrue(delivered.isEmpty());
        verify(a, "123456").andExpect(status().isUnauthorized());
        verify(b, "123456").andExpect(status().isUnauthorized());
        assertEquals(2, challenges.count());
    }
    @Test void correctCodeMinimalProfileAndHashedCredentials() throws Exception {
        String token = login();
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.dni").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.direccion").doesNotExist()).andExpect(jsonPath("$.id").doesNotExist());
        assertFalse(challenges.findAll().getFirst().codeHash.contains(delivered.getLast()));
        assertEquals(tokens.hash(token), sessions.findAll().getFirst().tokenHash);
        assertNotEquals(token, sessions.findAll().getFirst().tokenHash);
    }
    @Test void incorrectBlockedAndReusedCodes() throws Exception {
        activate(ana); String id = request(ana.getDni()); String code = delivered.getLast();
        String bad = "000000".equals(code) ? "111111" : "000000";
        for (int i = 0; i < 5; i++) verify(id, bad).andExpect(status().isUnauthorized());
        assertEquals(5, challenges.findById(id).orElseThrow().attempts);
        verify(id, code).andExpect(status().isUnauthorized());
        clock.advance(61); String next = request(ana.getDni());
        verify(next, delivered.getLast()).andExpect(status().isOk());
        verify(next, delivered.getLast()).andExpect(status().isUnauthorized());
    }
    @Test void incorrectCodeDoesNotPreventLaterCorrectCode() throws Exception {
        activate(ana); String id = request(ana.getDni()); String code = delivered.getLast();
        verify(id, code.equals("000000") ? "111111" : "000000").andExpect(status().isUnauthorized());
        verify(id, code).andExpect(status().isOk());
        assertEquals(2, challenges.findById(id).orElseThrow().attempts);
    }
    @Test void concurrentFailuresCannotLoseAttemptsOrBypassBlock() throws Exception {
        activate(ana); String id = request(ana.getDni()); String code = delivered.getLast();
        String bad = code.equals("000000") ? "111111" : "000000";
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) calls.add(() -> {
                try { service.verify(id, bad, "198.51.100.15"); return true; }
                catch (AutenticacionRequeridaException e) { return false; }
            });
            for (var result : pool.invokeAll(calls)) assertFalse(result.get());
        }
        assertEquals(5, challenges.findById(id).orElseThrow().attempts);
        verify(id, code).andExpect(status().isUnauthorized());
    }
    @Test void verificationIpLimitIncludesUnknownChallenges() {
        for (int i = 0; i < 60; i++) assertThrows(AutenticacionRequeridaException.class,
                () -> service.verify(UUID.randomUUID().toString(), "000000", "198.51.100.16"));
        activate(ana);
        var request = service.requestCode(ana.getDni(), "198.51.100.17");
        assertThrows(AutenticacionRequeridaException.class,
                () -> service.verify(request.solicitudId(), delivered.getLast(), "198.51.100.16"));
        assertEquals(0, sessions.count());
    }
    @Test void expiredCodeAndResendInvalidation() throws Exception {
        activate(ana); String id = request(ana.getDni()); String code = delivered.getLast();
        clock.advance(300);
        verify(id, code).andExpect(status().isUnauthorized());
        String next = request(ana.getDni()); String nextCode = delivered.getLast();
        clock.advance(61); String latest = request(ana.getDni());
        verify(next, nextCode).andExpect(status().isUnauthorized());
        verify(latest, delivered.getLast()).andExpect(status().isOk());
    }
    @Test void identityLimitIncludesResendsAcrossIpsAndCooldown() throws Exception {
        activate(ana); request(ana.getDni()); request(ana.getDni()); assertEquals(1, delivered.size());
        for (int i = 0; i < 5; i++) {
            clock.advance(61);
            service.requestCode(ana.getDni(), "198.51.100." + i);
        }
        assertEquals(3, delivered.size());
    }
    @Test void ipLimitAppliesAcrossIdentities() {
        for (int i = 0; i < 30; i++) service.requestCode("" + (40000000 + i), "198.51.100.10");
        assertEquals(20, challenges.count());
    }
    @Test void concurrentVerificationIssuesExactlyOneSession() throws Exception {
        activate(ana); String id = request(ana.getDni()); String code = delivered.getLast();
        try (var pool = Executors.newFixedThreadPool(8)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) results.add(pool.submit(() -> {
                start.await();
                try { service.verify(id, code, "198.51.100.11"); return true; }
                catch (AutenticacionRequeridaException e) { return false; }
            }));
            start.countDown(); int success = 0;
            for (var r : results) if (r.get(15, TimeUnit.SECONDS)) success++;
            assertEquals(1, success); assertEquals(1, sessions.count());
        }
    }
    @Test void concurrentRequestsEnforceCooldown() throws Exception {
        activate(ana);
        try (var pool = Executors.newFixedThreadPool(8)) {
            List<Callable<PortalService.ChallengeResponse>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) calls.add(() -> service.requestCode(ana.getDni(), "198.51.100.12"));
            for (var result : pool.invokeAll(calls)) result.get();
        }
        assertEquals(1, delivered.size()); assertEquals(1, challenges.count());
    }
    @Test void expiredRevokedAndInstitutionallyRevokedSessions() throws Exception {
        String token = login();
        mvc.perform(post("/api/portal-bomberos/auth/cerrar-sesion").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        clock.advance(61); token = login(); clock.advance(900);
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        token = login(); service.activate(adminHeader, ana.getId(), "Revocacion ficticia 002", true);
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        activate(ana); // Reactivation must never resurrect a previous session.
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void changedEmailAndInactiveSocioInvalidateAccess() throws Exception {
        String token = login(); ana.setEmail("changed@example.test"); socios.saveAndFlush(ana);
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        clock.advance(61); request(ana.getDni()); assertEquals(1, delivered.size());
        activate(ana); clock.advance(61); String id = request(ana.getDni());
        token = mapper.readTree(verify(id, delivered.getLast()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
        ana.setEstado("INACTIVO"); socios.saveAndFlush(ana);
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    @Test void anotherInstitutionCannotActivateOrAuthenticate() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> activate(sameDniOtherClub));
        // Same DNI in another institution does not authorize the unverified Bomberos member.
        request(ana.getDni()); assertTrue(delivered.isEmpty());
        activate(ana); clock.advance(61); String id = request(ana.getDni());
        assertEquals(ana.getId(), challenges.findById(id).orElseThrow().socioId);
        String token = mapper.readTree(verify(id, delivered.getLast()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
        ana.setDni("55667788"); ana.setClub(otherClub); socios.saveAndFlush(ana);
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }
    void cuota(Socio s, String period, EstadoCuota state, LocalDate due) {
        var c = new Cuota(); c.setClub(s.getClub()); c.setSocio(s); c.setPeriodo(period);
        c.setImporte(new BigDecimal("1500.75")); c.setEstado(state); c.setVencimiento(due);
        cuotas.saveAndFlush(c);
    }
    @Test void onlyOwnRealCuotasAndStatesAreExposed() throws Exception {
        String token = login(); LocalDate today = LocalDate.now(clock);
        cuota(ana, "2026-01", EstadoCuota.PAGADA, today.minusDays(10));
        cuota(ana, "2026-02", EstadoCuota.PENDIENTE, today.plusDays(10));
        cuota(ana, "2026-03", EstadoCuota.PENDIENTE, today.minusDays(1));
        cuota(other, "2026-01", EstadoCuota.PENDIENTE, today);
        cuota(sameDniOtherClub, "2026-01", EstadoCuota.PENDIENTE, today);
        mvc.perform(get("/api/portal-bomberos/cuotas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[1].estado").value("VENCIDA"))
                .andExpect(jsonPath("$[2].estado").value("PAGADA"))
                .andExpect(jsonPath("$[0].importe").value(1500.75)).andExpect(jsonPath("$[0].socioDni").doesNotExist());
        mvc.perform(get("/api/portal-bomberos/cuotas").param("socioId", other.getId().toString())
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/portal-bomberos/cuotas/" + other.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/api/portal-bomberos/cuotas").header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content("{\"importe\":0}")).andExpect(status().is4xxClientError());
    }
    @Test void adminPermissionsRemainSeparate() throws Exception {
        String token = login();
        mvc.perform(get("/api/clubes").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clubes").header("Authorization", adminHeader)).andExpect(status().isOk());
        mvc.perform(get("/api/portal-bomberos/me").header("Authorization", adminHeader)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/portal-bomberos-administracion/socios/" + ana.getId() + "/correo-verificado")
                .header("Authorization", "Bearer " + token).contentType(APPLICATION_JSON)
                .content("{\"referenciaVerificacion\":\"acta ficticia\"}")).andExpect(status().isUnauthorized());
    }
    @Test void assistedActivationRequiresInstitutionAdminAndIsAudited() throws Exception {
        var professor = users.saveAndFlush(new Usuario("88776655", "Profesor", "Prueba", RolUsuario.PROFESOR,
                EstadoUsuario.ACTIVO, "unused", false));
        adminSessions.saveAndFlush(new SesionUsuario(tokens.hash("professor"), LocalDateTime.now().plusHours(1), professor));
        String path = "/api/portal-bomberos-administracion/socios/" + ana.getId() + "/correo-verificado";
        mvc.perform(post(path).header("Authorization", "Bearer professor").contentType(APPLICATION_JSON)
                .content("{\"referenciaVerificacion\":\"acta ficticia\"}")).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", adminHeader).contentType(APPLICATION_JSON)
                .content("{\"referenciaVerificacion\":\"acta ficticia\"}")).andExpect(status().isNoContent());
        assertEquals(1, emails.count());
        assertEquals(1, jdbc.queryForObject("select count(*) from auditoria where accion='ACTIVACION_PORTAL'", Integer.class));
    }
    @Test void cleanupRemovesExpiredCredentials() throws Exception {
        login(); clock.advance(901); service.cleanup();
        assertEquals(0, challenges.count()); assertEquals(0, sessions.count());
    }
    @Test void corsAllowsOnlyConfiguredPortalOrigin() throws Exception {
        mvc.perform(options("/api/portal-bomberos/cuotas").header("Origin", "https://bomberos.example.test")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://bomberos.example.test"));
        mvc.perform(options("/api/portal-bomberos/cuotas").header("Origin", "https://untrusted.example.test")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
    }
}
