package com.redclubes.backend.gestion;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.socios.Socio;
import com.redclubes.backend.socios.SocioNoEncontradoException;
import com.redclubes.backend.socios.SocioRepository;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CobranzaService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ClubRepository clubRepository;
    private final SocioRepository socioRepository;
    private final CuotaRepository cuotaRepository;
    private final PagoRepository pagoRepository;
    private final AuditoriaService auditoriaService;

    public CobranzaService(
            ClubRepository clubRepository,
            SocioRepository socioRepository,
            CuotaRepository cuotaRepository,
            PagoRepository pagoRepository,
            AuditoriaService auditoriaService
    ) {
        this.clubRepository = clubRepository;
        this.socioRepository = socioRepository;
        this.cuotaRepository = cuotaRepository;
        this.pagoRepository = pagoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public List<CuotaResponse> listarCuotas(Long clubId, String periodo, EstadoCuota estado, Long socioId) {
        actualizarVencidas(clubId, LocalDate.now(BUSINESS_ZONE));
        return cuotaRepository.findByClubId(clubId).stream()
                .filter(cuota -> cuota.getSocio() != null && cuota.getSocio().getClub() != null
                        && cuota.getSocio().getClub().getId().equals(clubId))
                .filter(cuota -> periodo == null || periodo.equals(cuota.getPeriodo()))
                .filter(cuota -> estado == null || estado == cuota.getEstado())
                .filter(cuota -> socioId == null || socioId.equals(cuota.getSocio().getId()))
                .sorted(Comparator.comparing(Cuota::getVencimiento).reversed())
                .map(CuotaResponse::desde)
                .toList();
    }

    @Transactional
    public CuotaResponse crearCuota(Long clubId, CrearCuotaRequest request, Usuario actor) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Socio socio = socioRepository.findByIdAndClubId(request.socioId(), clubId)
                .orElseThrow(() -> new SocioNoEncontradoException(request.socioId()));
        if (cuotaRepository.existsByClubIdAndSocioIdAndPeriodo(clubId, socio.getId(), request.periodo())) {
            throw new IllegalArgumentException("Ya existe una cuota para el socio y periodo");
        }
        Cuota guardada = cuotaRepository.save(nuevaCuota(
                club, socio, request.periodo(), request.importe(),
                request.fechaEmision() == null ? inicioDelPeriodo(request.periodo()) : request.fechaEmision(),
                request.vencimiento()
        ));
        auditoriaService.registrar(actor, clubId, "ALTA", "CUOTA", guardada.getId(), "Periodo=" + request.periodo());
        return CuotaResponse.desde(guardada);
    }

    @Transactional
    public GeneracionCuotasResponse generarCuotas(Long clubId, GenerarCuotasRequest request, Usuario actor) {
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ClubNoEncontradoException(clubId));
        List<CuotaResponse> creadas = new ArrayList<>();
        int omitidas = 0;
        for (Socio socio : socioRepository.findByClubId(clubId)) {
            if (!"ACTIVO".equals(socio.getEstado())
                    || cuotaRepository.existsByClubIdAndSocioIdAndPeriodo(clubId, socio.getId(), request.periodo())) {
                omitidas++;
                continue;
            }
            Cuota cuota = nuevaCuota(club, socio, request.periodo(), request.importe(),
                    inicioDelPeriodo(request.periodo()), request.vencimiento());
            creadas.add(CuotaResponse.desde(cuotaRepository.save(cuota)));
        }
        auditoriaService.registrar(actor, clubId, "GENERACION", "CUOTA", null,
                "Periodo=" + request.periodo() + "; creadas=" + creadas.size() + "; omitidas=" + omitidas);
        return new GeneracionCuotasResponse(request.periodo(), creadas.size(), omitidas, creadas);
    }

    @Transactional
    public PagoResponse registrarPago(Long clubId, Long cuotaId, RegistrarPagoRequest request, Usuario responsable) {
        Cuota cuota = cuotaRepository.findLockedByIdAndClubId(cuotaId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("Cuota no encontrada"));
        validarCuotaDelClub(cuota, clubId);
        if (cuota.getEstado() == EstadoCuota.PAGADA
                || pagoRepository.existsByClubIdAndCuotaIdAndEstado(clubId, cuotaId, EstadoPago.ACTIVO)) {
            throw new IllegalArgumentException("La cuota ya tiene un pago activo");
        }
        if (cuota.getEstado() == EstadoCuota.ANULADA) {
            throw new IllegalArgumentException("No se puede pagar una cuota anulada");
        }

        Pago pago = new Pago();
        pago.setClub(cuota.getClub());
        pago.setCuota(cuota);
        pago.setImporte(cuota.getImporte());
        pago.setFechaPago(LocalDateTime.now(BUSINESS_ZONE));
        pago.setMedioPago(request.medioPago());
        pago.setUsuarioResponsable(responsable);
        pago.setObservaciones(request.observaciones());
        pago.setEstado(EstadoPago.ACTIVO);
        pagoRepository.save(pago);
        cuota.setEstado(EstadoCuota.PAGADA);
        cuotaRepository.save(cuota);
        auditoriaService.registrar(responsable, clubId, "REGISTRO", "PAGO", pago.getId(),
                "Cuota=" + cuotaId + "; medio=" + request.medioPago());
        return PagoResponse.desde(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listarPagos(Long clubId, Long cuotaId) {
        cuotaRepository.findByIdAndClubId(cuotaId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("Cuota no encontrada"));
        return pagoRepository.findByClubIdAndCuotaIdOrderByIdDesc(clubId, cuotaId).stream()
                .map(PagoResponse::desde)
                .toList();
    }

    @Transactional
    public PagoResponse anularPago(Long clubId, Long cuotaId, Long pagoId, AnularPagoRequest request, Usuario responsable) {
        Cuota cuota = cuotaRepository.findLockedByIdAndClubId(cuotaId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("Cuota no encontrada"));
        Pago pago = pagoRepository.findByIdAndClubIdAndCuotaId(pagoId, clubId, cuotaId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        if (pago.getEstado() == EstadoPago.ANULADO) {
            throw new IllegalArgumentException("El pago ya esta anulado");
        }
        pago.setEstado(EstadoPago.ANULADO);
        pago.setFechaAnulacion(LocalDateTime.now(BUSINESS_ZONE));
        pago.setUsuarioAnulacion(responsable);
        pago.setObservaciones(unirObservaciones(pago.getObservaciones(), "Anulacion: " + request.motivo()));
        cuota.setEstado(cuota.getVencimiento().isBefore(LocalDate.now(BUSINESS_ZONE))
                ? EstadoCuota.VENCIDA : EstadoCuota.PENDIENTE);
        cuotaRepository.save(cuota);
        Pago guardado = pagoRepository.save(pago);
        auditoriaService.registrar(responsable, clubId, "ANULACION", "PAGO", pagoId, "Cuota=" + cuotaId);
        return PagoResponse.desde(guardado);
    }

    @Scheduled(cron = "${redclubes.cuotas.vencimiento-cron:0 5 0 * * *}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void actualizarVencidasProgramadas() {
        LocalDate hoy = LocalDate.now(BUSINESS_ZONE);
        cuotaRepository.findAll().stream()
                .filter(cuota -> cuota.getEstado() == EstadoCuota.PENDIENTE && cuota.getVencimiento().isBefore(hoy))
                .forEach(cuota -> cuota.setEstado(EstadoCuota.VENCIDA));
    }

    private void actualizarVencidas(Long clubId, LocalDate hoy) {
        cuotaRepository.findByClubId(clubId).stream()
                .filter(cuota -> cuota.getEstado() == EstadoCuota.PENDIENTE && cuota.getVencimiento().isBefore(hoy))
                .forEach(cuota -> cuota.setEstado(EstadoCuota.VENCIDA));
    }

    private Cuota nuevaCuota(Club club, Socio socio, String periodo, java.math.BigDecimal importe,
                             LocalDate fechaEmision, LocalDate vencimiento) {
        if (vencimiento.isBefore(fechaEmision)) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a la emision");
        }
        Cuota cuota = new Cuota();
        cuota.setClub(club);
        cuota.setSocio(socio);
        cuota.setPeriodo(periodo);
        cuota.setImporte(importe);
        cuota.setFechaEmision(fechaEmision);
        cuota.setVencimiento(vencimiento);
        cuota.setEstado(EstadoCuota.PENDIENTE);
        return cuota;
    }

    private LocalDate inicioDelPeriodo(String periodo) {
        return YearMonth.parse(periodo).atDay(1);
    }

    private void validarCuotaDelClub(Cuota cuota, Long clubId) {
        if (cuota.getSocio() == null || cuota.getSocio().getClub() == null
                || !cuota.getSocio().getClub().getId().equals(clubId)) {
            throw new IllegalArgumentException("La cuota no pertenece a un socio del club");
        }
    }

    private String unirObservaciones(String original, String agregado) {
        return original == null || original.isBlank() ? agregado : original + " | " + agregado;
    }
}
