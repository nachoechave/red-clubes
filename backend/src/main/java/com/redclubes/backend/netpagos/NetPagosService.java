package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.JsonNode;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.clubes.EstadoClub;
import com.redclubes.backend.gestion.*;
import com.redclubes.backend.portal.*;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.usuarios.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.redclubes.backend.netpagos.NetPaymentIntent.State.*;

@Service
public class NetPagosService {
    public record ItemView(Long cuotaId, BigDecimal importe, boolean acreditado) {}
    public record View(String id, NetPaymentIntent.State estado, BigDecimal total, String urlPago,
                       boolean simulado, String incidencia, List<ItemView> cuotas) {}
    public record PageView(List<View> contenido, int pagina, int totalPaginas, long total) {}
    private record Prepared(String id, boolean dispatch, NetPagosGateway.Create payload) {}
    private record Query(String id, String uuid, boolean execute) {}
    private final NetPagosConfig config;
    private final NetPaymentIntentRepository intents;
    private final NetPaymentItemRepository items;
    private final NetPaymentBarcodeRepository barcodes;
    private final NetPaymentReservationRepository reservations;
    private final NetWebhookEventRepository events;
    private final PortalSocioRepository socios;
    private final CuotaRepository cuotas;
    private final CobranzaService cobranza;
    private final SessionTokenService tokens;
    private final NetPagosGateway gateway;
    private final Clock clock;
    private final AuthService auth;
    private final UsuarioRepository users;
    private final AuditoriaService audit;
    private final TransactionTemplate tx;

    NetPagosService(NetPagosConfig config, NetPaymentIntentRepository intents, NetPaymentItemRepository items,
            NetPaymentBarcodeRepository barcodes, NetPaymentReservationRepository reservations,
            NetWebhookEventRepository events, PortalSocioRepository socios, CuotaRepository cuotas,
            CobranzaService cobranza, SessionTokenService tokens, NetPagosGateway gateway, Clock clock,
            AuthService auth, UsuarioRepository users, AuditoriaService audit, PlatformTransactionManager manager) {
        this.config = config; this.intents = intents; this.items = items; this.barcodes = barcodes;
        this.reservations = reservations; this.events = events; this.socios = socios; this.cuotas = cuotas;
        this.cobranza = cobranza; this.tokens = tokens; this.gateway = gateway; this.clock = clock;
        this.auth = auth; this.users = users; this.audit = audit;
        this.tx = new TransactionTemplate(manager);
        this.tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public View create(PortalService.Principal principal, List<Long> selected, String key, String gender) {
        enabled();
        if (selected == null || selected.isEmpty() || selected.size() > 10 || selected.stream().anyMatch(Objects::isNull)
                || selected.stream().anyMatch(id -> id <= 0) || new HashSet<>(selected).size() != selected.size()
                || gender == null || !gender.matches("[123]")) throw bad("Seleccion o genero de documento invalido");
        NetPagosHttpGateway.uuid(key);
        List<Long> ordered = selected.stream().sorted().toList();
        Prepared prepared = tx.execute(s -> prepare(principal, ordered, key, gender));
        if (prepared.dispatch()) {
            // The local intent is committed before I/O. A lost response must NEVER cause another POST.
            try {
                var created = gateway.create(prepared.payload());
                tx.executeWithoutResult(s -> {
                    var intent = lock(prepared.id());
                    if (intent.state != CREATING) throw conflict("La intencion requiere revision");
                    intent.providerUuid = NetPagosHttpGateway.uuid(created.uuid());
                    intent.checkoutUrl = created.url(); intent.state = READY;
                    intents.flush();
                });
            } catch (RuntimeException e) {
                tx.executeWithoutResult(s -> { var intent = lock(prepared.id()); intent.state = UNKNOWN; intent.issue = "CREATE_OUTCOME_UNKNOWN"; });
            }
        }
        return view(principal, prepared.id());
    }

    private Prepared prepare(PortalService.Principal principal, List<Long> selected, String key, String gender) {
        if (!principal.clubId().equals(config.clubId)) throw new AccesoDenegadoException();
        Socio socio = socios.lock(principal.socioId(), config.clubId).orElseThrow(AccesoDenegadoException::new);
        if (!"ACTIVO".equals(socio.getEstado()) || socio.getClub().getEstado() != EstadoClub.ACTIVO) throw new AccesoDenegadoException();
        String hash = tokens.hash(selected + ":" + gender);
        var previous = intents.findByClubIdAndSocioIdAndRequestKey(config.clubId, socio.getId(), key);
        if (previous.isPresent()) {
            if (!previous.get().requestHash.equals(hash)) throw conflict("La clave ya corresponde a otra seleccion");
            return new Prepared(previous.get().id, false, null);
        }
        if (intents.countBySocioIdAndCreatedAtAfter(socio.getId(), clock.instant().minusSeconds(900)) >= 10)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas intenciones de pago");
        var intent = new NetPaymentIntent(); intent.id = UUID.randomUUID().toString();
        intent.clubId = config.clubId; intent.socioId = socio.getId(); intent.requestKey = key; intent.requestHash = hash;
        intent.payerHash = tokens.hash("payer:" + socio.getDni()); intent.gender = gender;
        intent.providerMode = config.mode.name(); intent.createdAt = clock.instant(); intent.state = CREATING;
        var lines = new ArrayList<NetPaymentItem>();
        for (Long id : selected) {
            Cuota cuota = cuotas.findLockedByIdAndClubId(id, config.clubId).orElseThrow(() -> bad("Cuota no disponible"));
            if (cuota.getSocio() == null || !socio.getId().equals(cuota.getSocio().getId())
                    || cuota.getEstado() == EstadoCuota.ANULADA || cuota.getEstado() == EstadoCuota.PAGADA) throw bad("Cuota no disponible");
            if (reservations.existsById(id)) throw conflict("Una cuota ya tiene un pago en curso o por revisar");
            BigDecimal balance = cobranza.consultarSaldo(cuota).pendiente().setScale(2, RoundingMode.UNNECESSARY);
            var barcode = barcodes.findById(id).orElseThrow(() -> conflict("Falta configurar la barra con el convenio"));
            if (!barcode.clubId.equals(config.clubId) || barcode.amount.compareTo(balance) != 0 || balance.signum() <= 0
                    || barcode.validUntil.isBefore(today())) throw conflict("La barra debe actualizarse para el saldo o vencimiento actual");
            var item = new NetPaymentItem(); item.intentId = intent.id; item.cuotaId = id; item.amount = balance;
            item.barcode = barcode.barcode; item.service = barcode.service; item.detail = "Cuota " + cuota.getPeriodo(); lines.add(item);
        }
        intent.total = lines.stream().map(i -> i.amount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        if (intent.total.precision() > 12) throw bad("Importe total fuera de rango");
        intents.saveAndFlush(intent);
        for (var item : lines) {
            items.save(item); var reserve = new NetPaymentReservation(); reserve.cuotaId = item.cuotaId; reserve.intentId = intent.id;
            reservations.save(reserve);
        }
        var payer = new NetPagosGateway.Payer(socio.getNombre(), socio.getApellido(), socio.getEmail(), "1", socio.getDni(), gender, true);
        return new Prepared(intent.id, true, new NetPagosGateway.Create(payer, lines.stream()
                .map(i -> new NetPagosGateway.Payment(i.amount.toPlainString(), i.detail, i.barcode, i.service)).toList()));
    }

    public View view(PortalService.Principal principal, String id) {
        return tx.execute(s -> { var intent = own(principal, id); return response(intent); });
    }
    public View refresh(PortalService.Principal principal, String id) {
        enabled();
        Query query = tx.execute(s -> {
            var intent = lock(id); requireOwner(principal, intent);
            if (!intent.providerMode.equals(config.mode.name())) throw conflict("La intencion pertenece a otro ambiente");
            boolean execute = intent.providerUuid != null && (intent.nextQueryAt == null || !intent.nextQueryAt.isAfter(clock.instant()));
            if (execute) intent.nextQueryAt = clock.instant().plusSeconds(30);
            return new Query(id, intent.providerUuid, execute);
        });
        if (query.execute()) {
            NetPagosGateway.Status result;
            try { result = gateway.status(query.uuid()); }
            catch (RuntimeException e) {
                tx.executeWithoutResult(s -> lock(id).issue = "QUERY_UNAVAILABLE"); return view(principal, id);
            }
            tx.executeWithoutResult(s -> applyVerified(lock(id), result));
        }
        return view(principal, id);
    }

    private void applyVerified(NetPaymentIntent intent, NetPagosGateway.Status result) {
        var lines = items.findByIntentIdOrderByCuotaIdAsc(intent.id);
        List<NetPagosGateway.Item> matches;
        try { matches = validate(intent, lines, result); }
        catch (IllegalArgumentException e) { intent.state = intent.state == APPLIED ? APPLIED : REVIEW; intent.issue = "PROVIDER_DATA_MISMATCH"; return; }
        String state = result.status().toLowerCase(Locale.ROOT);
        if (intent.state == APPLIED) {
            if (!state.equals("finalizado") && !state.equals("parcial")) intent.issue = "LATE_STATE_REQUIRES_REVIEW";
            else for (int n = 0; n < lines.size(); n++) {
                if (isPaid(state, matches.get(n)) != (lines.get(n).pagoId != null)) intent.issue = "LATE_ITEM_RESULT_REQUIRES_REVIEW";
            }
            return;
        }
        if ((intent.state == CONFIRMED && !state.equals("finalizado")) || (intent.state == PARTIAL && !state.equals("parcial"))) {
            intent.state = REVIEW; intent.issue = "LATE_STATE_REQUIRES_REVIEW"; return;
        }
        if (intent.state == CANCELLED && !state.equals("cancelado")) { intent.state = REVIEW; intent.issue = "LATE_STATE_REQUIRES_REVIEW"; return; }
        if (state.equals("instanciado") || state.equals("proceso")) {
            if (intent.state != CONFIRMED && intent.state != PARTIAL) intent.state = state.equals("proceso") ? PROCESSING : READY;
            return;
        }
        if (state.equals("cancelado")) {
            if (matches.stream().anyMatch(i -> Boolean.TRUE.equals(i.paid()))) { intent.state = REVIEW; intent.issue = "INCONSISTENT_CANCELLED"; return; }
            intent.state = CANCELLED; intent.issue = null; reservations.deleteByIntentId(intent.id); return;
        }
        if (!state.equals("finalizado") && !state.equals("parcial")) { intent.state = REVIEW; intent.issue = "UNKNOWN_PROVIDER_STATE"; return; }
        if (state.equals("parcial") && (matches.stream().anyMatch(i -> i.paid() == null)
                || matches.stream().noneMatch(i -> Boolean.TRUE.equals(i.paid()))
                || matches.stream().allMatch(i -> Boolean.TRUE.equals(i.paid())))) {
            intent.state = REVIEW; intent.issue = "PARTIAL_REQUIRES_VERIFIED_ITEM_RESULTS"; return;
        }
        if (state.equals("finalizado") && matches.stream().anyMatch(i -> Boolean.FALSE.equals(i.paid()))) {
            intent.state = REVIEW; intent.issue = "INCONSISTENT_FINALIZED"; return;
        }
        intent.state = state.equals("parcial") ? PARTIAL : CONFIRMED; intent.issue = null;
        if (!config.settlementEnabled) { intent.issue = "SETTLEMENT_DISABLED"; return; }
        var actor = users.findById(config.actorId).filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .filter(u -> auth.puedeOperarClub(u, intent.clubId)).orElse(null);
        if (actor == null) { intent.state = REVIEW; intent.issue = "SETTLEMENT_ACTOR_UNAVAILABLE"; return; }
        // Acquire every cuota in the same order used by creation. Check the entire cart before any credit.
        for (int n = 0; n < lines.size(); n++) {
            var line = lines.get(n);
            Cuota cuota = cuotas.findLockedByIdAndClubId(line.cuotaId, intent.clubId).orElse(null);
            if (cuota == null || cuota.getSocio() == null || !cuota.getSocio().getId().equals(intent.socioId)
                    || !cuota.getSocio().getClub().getId().equals(intent.clubId) || cuota.getEstado() == EstadoCuota.ANULADA
                    || reservations.findById(line.cuotaId).filter(r -> r.intentId.equals(intent.id)).isEmpty()
                    || (isPaid(state, matches.get(n)) && cobranza.consultarSaldo(cuota).pendiente().compareTo(line.amount) != 0)) {
                intent.state = REVIEW; intent.issue = "LOCAL_BALANCE_OR_OWNERSHIP_CHANGED"; return;
            }
        }
        for (int n = 0; n < lines.size(); n++) {
            var line = lines.get(n);
            if (isPaid(state, matches.get(n)) && line.pagoId == null) {
                var pago = cobranza.registrarPagoNetPagos(intent.clubId, line.cuotaId, line.amount, actor,
                        "netpagos:" + intent.providerUuid + ":" + line.id);
                line.pagoId = pago.id();
            }
        }
        reservations.deleteByIntentId(intent.id); intent.state = APPLIED;
    }
    private boolean isPaid(String state, NetPagosGateway.Item item) { return state.equals("finalizado") || Boolean.TRUE.equals(item.paid()); }
    private List<NetPagosGateway.Item> validate(NetPaymentIntent intent, List<NetPaymentItem> lines, NetPagosGateway.Status status) {
        if (!Objects.equals(intent.providerUuid, status.uuid()) || !tokens.hash("payer:" + status.documentNumber()).equals(intent.payerHash)
                || money(status.totalAmount()).compareTo(intent.total) != 0 || status.payments() == null || status.payments().size() != lines.size())
            throw new IllegalArgumentException();
        var result = new ArrayList<NetPagosGateway.Item>();
        var remaining = new ArrayList<>(status.payments());
        for (var line : lines) {
            var matching = remaining.stream().filter(i -> line.barcode.equals(i.barcode()) && line.service.equals(i.service())).toList();
            if (matching.size() != 1 || money(matching.getFirst().amount()).compareTo(line.amount) != 0) throw new IllegalArgumentException();
            result.add(matching.getFirst()); remaining.remove(matching.getFirst());
        }
        return result;
    }
    static BigDecimal money(String text) {
        if (text == null || !text.matches("[0-9]{1,10}\\.[0-9]{2}")) throw new IllegalArgumentException("Importe del proveedor invalido");
        return new BigDecimal(text);
    }

    public void registerBarcode(String header, Long cuotaId, String barcode, String service, LocalDate until, String evidence) {
        enabled(); auth.exigirAdministradorDeClub(header, config.clubId);
        tx.executeWithoutResult(s -> {
            var cuota = cuotas.findLockedByIdAndClubId(cuotaId, config.clubId).orElseThrow(() -> bad("Cuota no disponible"));
            if (reservations.existsById(cuotaId)) throw conflict("Existe una intencion en curso");
            if (until == null || until.isBefore(today()) || barcode == null || !barcode.matches("[0-9]{1,128}")
                    || service == null || !service.matches("[A-Za-z0-9._:-]{1,80}") || evidence == null || evidence.isBlank()) throw bad("Datos de convenio invalidos");
            BigDecimal balance = cobranza.consultarSaldo(cuota).pendiente().setScale(2, RoundingMode.UNNECESSARY);
            if (balance.signum() <= 0 || cuota.getEstado() == EstadoCuota.ANULADA) throw conflict("Cuota sin saldo cobrable");
            var row = new NetPaymentBarcode(); row.cuotaId = cuotaId; row.clubId = config.clubId;
            row.barcode = barcode; row.service = service; row.amount = balance; row.validUntil = until; barcodes.save(row);
            audit.registrar(auth.obtenerUsuarioAutenticado(header), config.clubId, "NET_BARRA", "CUOTA", cuotaId,
                    "Referencia de convenio=" + evidence);
        });
    }
    public View adminRefresh(String header, String id) {
        auth.exigirAdministradorDeClub(header, config.clubId);
        PortalService.Principal principal = tx.execute(s -> { var i = lock(id); if (!i.clubId.equals(config.clubId)) throw new AccesoDenegadoException();
            return new PortalService.Principal(i.socioId, i.clubId, ""); });
        return refresh(principal, id);
    }
    public PageView reviewQueue(String header, NetPaymentIntent.State state, int page) {
        auth.exigirAdministradorDeClub(header, config.clubId);
        if (page < 0 || page > 100000) throw bad("Pagina invalida");
        return tx.execute(s -> {
            var result = intents.findByClubIdAndState(config.clubId, state,
                    org.springframework.data.domain.PageRequest.of(page, 20, org.springframework.data.domain.Sort.by("createdAt").descending()));
            return new PageView(result.getContent().stream().map(this::response).toList(), page, result.getTotalPages(), result.getTotalElements());
        });
    }
    public void reconcilePending() {
        if (config.mode == NetPagosConfig.Mode.OFF) return;
        var pending = intents.pending(config.clubId, List.of(READY, PROCESSING, CONFIRMED, PARTIAL), clock.instant(),
                org.springframework.data.domain.PageRequest.of(0, 5));
        for (var i : pending) {
            try { refresh(new PortalService.Principal(i.socioId, i.clubId, ""), i.id); }
            catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(NetPagosService.class).warn("No se pudo conciliar una intencion NET Pagos; revisar la bandeja administrativa");
            }
        }
    }
    public View recover(String header, String id, String uuid) {
        enabled(); auth.exigirAdministradorDeClub(header, config.clubId); NetPagosHttpGateway.uuid(uuid);
        tx.executeWithoutResult(s -> requireRecoverable(lock(id)));
        var result = gateway.status(uuid);
        var principal = tx.execute(s -> {
            var i = lock(id); requireRecoverable(i);
            i.providerUuid = uuid;
            try { validate(i, items.findByIntentIdOrderByCuotaIdAsc(id), result); }
            catch (IllegalArgumentException e) { throw conflict("La referencia no coincide con la intencion"); }
            applyVerified(i, result);
            audit.registrar(auth.obtenerUsuarioAutenticado(header), config.clubId, "NET_RECUPERACION", "SOCIO", i.socioId, "Intencion=" + id);
            return new PortalService.Principal(i.socioId, i.clubId, "");
        });
        return view(principal, id);
    }
    private void requireRecoverable(NetPaymentIntent i) {
        if (!i.clubId.equals(config.clubId)) throw new AccesoDenegadoException();
        if (!i.providerMode.equals(config.mode.name())) throw conflict("La intencion pertenece a otro ambiente");
        if (i.providerUuid != null || (i.state != UNKNOWN && i.state != CREATING)) throw conflict("La intencion ya tiene referencia");
    }
    public void simulate(String header, String id, String state, List<Boolean> paid) {
        auth.exigirAdministradorDeClub(header, config.clubId);
        if (config.mode != NetPagosConfig.Mode.SIMULATED || !(gateway instanceof NetPagosSimulator simulator)) throw unavailable();
        tx.executeWithoutResult(s -> {
            var intent = lock(id); if (!intent.clubId.equals(config.clubId)) throw new AccesoDenegadoException();
            if (intent.providerUuid == null) throw conflict("La intencion aun no tiene referencia externa");
            simulator.advance(intent.providerUuid, state, paid);
            audit.registrar(auth.obtenerUsuarioAutenticado(header), config.clubId, "NET_SIMULACION", "SOCIO", intent.socioId, "Intencion=" + id);
        });
    }
    public void webhook(JsonNode root) {
        if (!config.webhookEnabled || config.mode == NetPagosConfig.Mode.OFF) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String uuid;
        try { uuid = NetPagosHttpGateway.uuid(root.path("preorder").path("uuid").asText()); }
        catch (RuntimeException e) { throw bad("Notificacion invalida"); }
        String status = root.path("status").asText();
        if (!List.of("Instanciado", "Finalizado", "Cancelado", "Parcial").contains(status)) throw bad("Estado no reconocido");
        var located = intents.findByProviderUuid(uuid);
        if (located.isEmpty()) return; // Uniform acknowledgement, no orphan insertion or payer retention.
        tx.executeWithoutResult(s -> {
            var intent = lock(located.get().id);
            String fingerprint = tokens.hash(uuid + ":" + status);
            if (events.existsById(fingerprint)) return;
            var event = new NetWebhookEvent(); event.fingerprint = fingerprint; event.intentId = intent.id;
            event.receivedAt = clock.instant(); event.reportedStatus = status; events.save(event);
            // Unauthenticated webhook is only a hint. No balances, provider truth or state change here.
        });
    }

    private NetPaymentIntent own(PortalService.Principal principal, String id) {
        var intent = intents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); requireOwner(principal, intent); return intent;
    }
    private void requireOwner(PortalService.Principal principal, NetPaymentIntent i) {
        if (!i.clubId.equals(principal.clubId()) || !i.socioId.equals(principal.socioId()) || !i.clubId.equals(config.clubId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    private NetPaymentIntent lock(String id) { return intents.lock(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private View response(NetPaymentIntent i) {
        return new View(i.id, i.state, i.total, i.checkoutUrl, i.providerMode.equals("SIMULATED"), i.issue,
                items.findByIntentIdOrderByCuotaIdAsc(i.id).stream().map(n -> new ItemView(n.cuotaId, n.amount, n.pagoId != null)).toList());
    }
    private void enabled() { if (config.mode == NetPagosConfig.Mode.OFF) throw unavailable(); }
    private LocalDate today() { return LocalDate.now(clock.withZone(ZoneId.of("America/Argentina/Buenos_Aires"))); }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "NET Pagos no esta habilitado para operar"); }
}
