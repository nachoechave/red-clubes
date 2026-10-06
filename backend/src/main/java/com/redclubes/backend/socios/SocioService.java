package com.redclubes.backend.socios;

import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubNoEncontradoException;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SocioService {

    private final SocioRepository socioRepository;
    private final ClubRepository clubRepository;
    private final AuditoriaService auditoriaService;

    public SocioService(SocioRepository socioRepository, ClubRepository clubRepository, AuditoriaService auditoriaService) {
        this.socioRepository = socioRepository;
        this.clubRepository = clubRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<SocioResponse> listarSociosPorClub(Long clubId) {
        return socioRepository.findByClubIdOrderByIdAsc(clubId).stream()
                .map(SocioResponse::from)
                .toList();
    }

    @Transactional
    public SocioResponse crearSocioEnClub(Long clubId, CrearSocioRequest request, Usuario actor) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNoEncontradoException(clubId));
        Socio socio = new Socio();
        socio.setClub(club);
        aplicarCambios(socio, request.nombre(), request.apellido(), request.dni(), request.estado(),
                request.telefono(), request.email(), request.fechaNacimiento(), request.direccion(),
                request.emergenciaNombre(), request.emergenciaTelefono(), request.emergenciaRelacion());
        validarDniDisponible(clubId, request.dni(), null);
        completarFechaAlta(socio);
        completarNumeroSocio(clubId, socio);
        Socio guardado = socioRepository.save(socio);
        auditoriaService.registrar(actor, clubId, "ALTA", "SOCIO", guardado.getId(), "Socio creado");
        return SocioResponse.from(guardado);
    }

    @Transactional
    public ImportacionSociosResponse importarSocios(Long clubId, String contenidoCsv, Usuario actor) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubNoEncontradoException(clubId));

        if (contenidoCsv == null || contenidoCsv.isBlank()) {
            return new ImportacionSociosResponse(
                    false, 0, 0, List.of(),
                    List.of(new ImportacionSociosError(1, "", "El archivo esta vacio"))
            );
        }

        String normalizado = contenidoCsv.replace("\r\n", "\n").replace('\r', '\n');
        String[] lineas = normalizado.split("\n");
        int cabeceraIndex = primeraLineaConContenido(lineas);
        if (cabeceraIndex < 0) {
            return new ImportacionSociosResponse(
                    false, 0, 0, List.of(),
                    List.of(new ImportacionSociosError(1, "", "El archivo no contiene datos"))
            );
        }

        String cabecera = quitarBom(lineas[cabeceraIndex]).trim();
        String separador = cabecera.contains(";") ? ";" : ",";
        String[] columnas = separarLinea(cabecera, separador);
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < columnas.length; i++) {
            indices.put(columnas[i].trim().toLowerCase(Locale.ROOT), i);
        }

        List<ImportacionSociosError> errores = new ArrayList<>();
        for (String requerida : List.of("nombre", "apellido", "dni")) {
            if (!indices.containsKey(requerida)) {
                errores.add(new ImportacionSociosError(
                        cabeceraIndex + 1, "", "Falta la columna obligatoria: " + requerida
                ));
            }
        }
        if (!errores.isEmpty()) {
            return new ImportacionSociosResponse(false, 0, 0, List.of(), errores);
        }

        List<Socio> existentes = socioRepository.findByClubIdOrderByIdAsc(clubId);
        Set<String> dnisExistentes = new HashSet<>();
        Set<Integer> numerosExistentes = new HashSet<>();
        int maxNumero = 0;
        for (Socio socio : existentes) {
            if (socio.getDni() != null) {
                dnisExistentes.add(socio.getDni());
            }
            if (socio.getNumeroSocio() != null) {
                numerosExistentes.add(socio.getNumeroSocio());
                maxNumero = Math.max(maxNumero, socio.getNumeroSocio());
            }
        }

        Set<String> dnisArchivo = new HashSet<>();
        Set<Integer> numerosSolicitadosArchivo = new HashSet<>();
        List<FilaSocioImportacion> filasValidas = new ArrayList<>();
        int totalFilas = 0;

        for (int i = cabeceraIndex + 1; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isBlank()) {
                continue;
            }
            totalFilas++;
            int numeroFila = i + 1;
            String[] valores = separarLinea(linea, separador);

            String nombre = valorColumna(valores, indices.get("nombre"));
            String apellido = valorColumna(valores, indices.get("apellido"));
            String dniOriginal = valorColumna(valores, indices.get("dni"));
            String dni = dniOriginal.replaceAll("\\D", "");
            String telefono = valorOpcional(valores, indices, "telefono");
            String email = valorOpcional(valores, indices, "email");
            String direccion = valorOpcional(valores, indices, "direccion");
            String estado = valorOpcional(valores, indices, "estado");
            String fechaNacimientoTexto = valorOpcional(valores, indices, "fecha_nacimiento");
            String fechaAltaTexto = valorOpcional(valores, indices, "fecha_alta");
            String numeroSocioTexto = valorOpcional(valores, indices, "numero_socio");

            if (nombre.length() < 2 || nombre.length() > 50) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El nombre debe tener entre 2 y 50 caracteres"));
                continue;
            }
            if (apellido.length() < 2 || apellido.length() > 50) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El apellido debe tener entre 2 y 50 caracteres"));
                continue;
            }
            if (!dni.matches("\\d{7,10}")) {
                errores.add(new ImportacionSociosError(numeroFila, dniOriginal, "DNI invalido"));
                continue;
            }
            if (dnisExistentes.contains(dni)) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El DNI ya esta registrado en la institucion"));
                continue;
            }
            if (!dnisArchivo.add(dni)) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El DNI esta duplicado dentro del archivo"));
                continue;
            }
            if (telefono.length() > 30) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El telefono no puede superar 30 caracteres"));
                continue;
            }
            if (direccion.length() > 200) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "La direccion no puede superar 200 caracteres"));
                continue;
            }
            if (email.length() > 120 || (!email.isBlank() && !emailValido(email))) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "Email invalido"));
                continue;
            }

            String estadoNormalizado = estado.isBlank() ? "ACTIVO" : estado.toUpperCase(Locale.ROOT);
            if (!Set.of("ACTIVO", "INACTIVO").contains(estadoNormalizado)) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "El estado debe ser ACTIVO o INACTIVO"));
                continue;
            }

            LocalDate fechaNacimiento = parsearFechaOpcional(fechaNacimientoTexto);
            if (!fechaNacimientoTexto.isBlank() && fechaNacimiento == null) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "Fecha de nacimiento invalida; usa YYYY-MM-DD"));
                continue;
            }
            if (fechaNacimiento != null && !fechaNacimiento.isBefore(LocalDate.now())) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "La fecha de nacimiento debe ser anterior a hoy"));
                continue;
            }

            LocalDate fechaAlta = parsearFechaOpcional(fechaAltaTexto);
            if (!fechaAltaTexto.isBlank() && fechaAlta == null) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "Fecha de alta invalida; usa YYYY-MM-DD"));
                continue;
            }
            if (fechaAlta != null && fechaAlta.isAfter(LocalDate.now())) {
                errores.add(new ImportacionSociosError(numeroFila, dni, "La fecha de alta no puede ser futura"));
                continue;
            }

            Integer numeroSocioSolicitado = null;
            if (!numeroSocioTexto.isBlank()) {
                try {
                    numeroSocioSolicitado = Integer.valueOf(numeroSocioTexto);
                    if (numeroSocioSolicitado <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException exception) {
                    errores.add(new ImportacionSociosError(numeroFila, dni, "Numero de socio invalido"));
                    continue;
                }
                if (numerosExistentes.contains(numeroSocioSolicitado)) {
                    errores.add(new ImportacionSociosError(numeroFila, dni, "El numero de socio ya existe en la institucion"));
                    continue;
                }
                if (!numerosSolicitadosArchivo.add(numeroSocioSolicitado)) {
                    errores.add(new ImportacionSociosError(numeroFila, dni, "El numero de socio esta duplicado dentro del archivo"));
                    continue;
                }
            }

            filasValidas.add(new FilaSocioImportacion(
                    numeroFila,
                    nombre.trim(),
                    apellido.trim(),
                    dni,
                    telefono,
                    email,
                    fechaNacimiento,
                    fechaAlta,
                    numeroSocioSolicitado,
                    direccion,
                    estadoNormalizado
            ));
        }

        if (totalFilas > 5000) {
            errores.add(new ImportacionSociosError(0, "", "El archivo supera el maximo de 5000 filas"));
        }
        if (totalFilas == 0 && errores.isEmpty()) {
            errores.add(new ImportacionSociosError(0, "", "El archivo no contiene socios"));
        }
        if (!errores.isEmpty()) {
            return new ImportacionSociosResponse(false, totalFilas, 0, List.of(), errores);
        }

        Set<Integer> numerosUsados = new HashSet<>(numerosExistentes);
        numerosUsados.addAll(numerosSolicitadosArchivo);
        int siguienteNumero = maxNumero + 1;

        List<ImportacionSociosItemResponse> importados = new ArrayList<>();
        for (FilaSocioImportacion fila : filasValidas) {
            Integer numeroSocio = fila.numeroSocio();
            if (numeroSocio == null) {
                while (numerosUsados.contains(siguienteNumero)) {
                    siguienteNumero++;
                }
                numeroSocio = siguienteNumero;
                numerosUsados.add(numeroSocio);
                siguienteNumero++;
            }

            Socio socio = new Socio();
            socio.setClub(club);
            socio.setNombre(fila.nombre());
            socio.setApellido(fila.apellido());
            socio.setDni(fila.dni());
            socio.setEstado(fila.estado());
            socio.setTelefono(vacioANull(fila.telefono()));
            socio.setEmail(vacioANull(fila.email()));
            socio.setFechaNacimiento(fila.fechaNacimiento());
            socio.setFechaAlta(fila.fechaAlta() == null ? LocalDate.now() : fila.fechaAlta());
            socio.setNumeroSocio(numeroSocio);
            socio.setDireccion(vacioANull(fila.direccion()));

            Socio guardado = socioRepository.save(socio);
            importados.add(new ImportacionSociosItemResponse(
                    fila.numeroFila(), fila.dni(), guardado.getId(), guardado.getNumeroSocio()
            ));
        }

        auditoriaService.registrar(
                actor,
                clubId,
                "IMPORTACION",
                "SOCIOS",
                null,
                "Filas=" + importados.size()
        );

        return new ImportacionSociosResponse(
                true, totalFilas, importados.size(), importados, List.of()
        );
    }

    @Transactional
    public SocioResponse actualizarSocioEnClub(Long clubId, Long id, ActualizarSocioRequest request, Usuario actor) {
        Socio socioExistente = socioRepository.findByIdAndClubId(id, clubId)
                            .orElseThrow(() -> new SocioNoEncontradoException(id));

        validarDniDisponible(clubId, request.dni(), id);
        aplicarCambios(socioExistente, request.nombre(), request.apellido(), request.dni(), request.estado(),
                request.telefono(), request.email(), request.fechaNacimiento(), request.direccion(),
                request.emergenciaNombre(), request.emergenciaTelefono(), request.emergenciaRelacion());

        Socio guardado = socioRepository.save(socioExistente);
        auditoriaService.registrar(actor, clubId, "MODIFICACION", "SOCIO", id, "Datos del socio actualizados");
        return SocioResponse.from(guardado);
    }

    private void completarFechaAlta(Socio socio) {
        if (socio.getFechaAlta() == null) {
            socio.setFechaAlta(LocalDate.now());
        }
    }

    private void completarNumeroSocio(Long clubId, Socio socio) {
        if (socio.getNumeroSocio() == null) {
            socio.setNumeroSocio(socioRepository.maxNumeroSocioPorClub(clubId) + 1);
        }
    }

    @Transactional
    public SocioResponse eliminarSocioEnClub(Long clubId, Long id, Usuario actor) {
        Socio socioExistente = socioRepository.findByIdAndClubId(id, clubId)
                .orElseThrow(() -> new SocioNoEncontradoException(id));

        socioExistente.setEstado("INACTIVO");

        Socio guardado = socioRepository.save(socioExistente);
        auditoriaService.registrar(actor, clubId, "BAJA", "SOCIO", id, "Socio marcado como inactivo");
        return SocioResponse.from(guardado);
    }

    private void aplicarCambios(
            Socio socio,
            String nombre,
            String apellido,
            String dni,
            String estado,
            String telefono,
            String email,
            LocalDate fechaNacimiento,
            String direccion,
            String emergenciaNombre,
            String emergenciaTelefono,
            String emergenciaRelacion
    ) {
        socio.setNombre(nombre.trim());
        socio.setApellido(apellido.trim());
        socio.setDni(dni.trim());
        socio.setEstado(estado);
        socio.setTelefono(telefono);
        socio.setEmail(email);
        socio.setFechaNacimiento(fechaNacimiento);
        socio.setDireccion(direccion);
        socio.setEmergenciaNombre(emergenciaNombre);
        socio.setEmergenciaTelefono(emergenciaTelefono);
        socio.setEmergenciaRelacion(emergenciaRelacion);
    }

    private int primeraLineaConContenido(String[] lineas) {
        for (int i = 0; i < lineas.length; i++) {
            if (!lineas[i].isBlank()) {
                return i;
            }
        }
        return -1;
    }

    private String quitarBom(String texto) {
        return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }

    private String[] separarLinea(String linea, String separador) {
        return linea.split(Pattern.quote(separador), -1);
    }

    private String valorColumna(String[] valores, Integer indice) {
        if (indice == null || indice < 0 || indice >= valores.length) {
            return "";
        }
        String valor = valores[indice].trim();
        if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
            valor = valor.substring(1, valor.length() - 1).trim();
        }
        return valor;
    }

    private String valorOpcional(String[] valores, Map<String, Integer> indices, String nombre) {
        return valorColumna(valores, indices.get(nombre));
    }

    private LocalDate parsearFechaOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean emailValido(String email) {
        return email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarDniDisponible(Long clubId, String dni, Long socioId) {
        boolean duplicado = socioId == null
                ? socioRepository.existsByClubIdAndDni(clubId, dni)
                : socioRepository.existsByClubIdAndDniAndIdNot(clubId, dni, socioId);
        if (duplicado) {
            throw new IllegalArgumentException("El DNI ya se encuentra registrado en el club");
        }
    }

    private record FilaSocioImportacion(
            int numeroFila,
            String nombre,
            String apellido,
            String dni,
            String telefono,
            String email,
            LocalDate fechaNacimiento,
            LocalDate fechaAlta,
            Integer numeroSocio,
            String direccion,
            String estado
    ) {
    }
}
