package com.redclubes.backend.socios;

import com.redclubes.backend.auditoria.AuditoriaService;
import com.redclubes.backend.clubes.Club;
import com.redclubes.backend.clubes.ClubRepository;
import com.redclubes.backend.usuarios.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Importa declaraciones históricas sin inventar importes, recibos ni fechas de pago.
 * Los estados se almacenan aparte del libro de cuotas y pagos contables.
 */
@Service
public class ImportacionIntegralService {
    private static final List<String> MESES = List.of("enero","febrero","marzo","abril",
            "mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre");

    private final SocioRepository socios;
    private final ClubRepository clubes;
    private final JdbcTemplate jdbc;
    private final AuditoriaService auditoria;

    public ImportacionIntegralService(SocioRepository socios, ClubRepository clubes, JdbcTemplate jdbc, AuditoriaService auditoria) {
        this.socios = socios;
        this.clubes = clubes;
        this.jdbc = jdbc;
        this.auditoria = auditoria;
    }

    public record ErrorFila(int fila, String mensaje) {}
    public record Resultado(boolean valida, boolean ejecutada, int sociosNuevos, int sociosExistentes,
                            int mesesPagados, int mesesPendientes, int mesesVacios,
                            int mesesOmitidos, List<ErrorFila> errores) {}
    private record Fila(int numero, String dni, String nombre, String apellido, Integer numeroSocio,
                        String telefono, String email, String direccion, String estado,
                        LocalDate nacimiento, LocalDate alta, List<String> meses) {}

    @Transactional
    public Resultado procesar(Long clubId, String csv, int anio, boolean confirmar, Usuario actor) {
        if (anio < 2000 || anio > LocalDate.now().getYear()) {
            throw new IllegalArgumentException("Año histórico fuera de rango");
        }
        // Bloqueo por club, antes de la validación y escritura, contra cargas simultáneas.
        Club club = clubes.findByIdForUpdate(clubId).orElseThrow(() ->
                new IllegalArgumentException("Institución inexistente"));
        List<ErrorFila> errores = new ArrayList<>();
        List<Fila> filas = new ArrayList<>();
        List<List<String>> records = parsear(csv);
        int cabecera = -1;
        Map<String,Integer> indices = new HashMap<>();
        for (int i=0;i<records.size();i++) {
            Map<String,Integer> prueba = new HashMap<>();
            List<String> values=records.get(i);
            for (int j=0;j<values.size();j++) prueba.put(normalizar(values.get(j)), j);
            if (prueba.containsKey("dni") && (prueba.containsKey("nombre") ||
                    prueba.containsKey("nombre y apellido"))) {
                cabecera=i; indices=prueba; break;
            }
        }
        for (int i=0; i<Math.max(0,cabecera); i++) {
            String titulo = String.join(" ",records.get(i));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)a[nñ]o\\s+(20\\d{2})").matcher(titulo);
            if (m.find() && Integer.parseInt(m.group(1)) != anio) {
                errores.add(new ErrorFila(i+1,"El año del encabezado no coincide con el año seleccionado"));
            }
        }
        if (cabecera < 0) return new Resultado(false,false,0,0,0,0,0,0,
                List.of(new ErrorFila(1,"No se encontró cabecera con DNI y nombre")));
        boolean completo = indices.containsKey("nombre") && indices.containsKey("apellido");
        if (!completo && !indices.containsKey("nombre y apellido")) {
            return new Resultado(false,false,0,0,0,0,0,0,
                    List.of(new ErrorFila(cabecera+1,"Falta nombre/apellido")));
        }
        Set<String> dnis = new HashSet<>();
        Set<Integer> numeros = new HashSet<>();
        int vacios=0, pagados=0, pendientes=0;
        for (int i=cabecera+1;i<records.size();i++) {
            List<String> row=records.get(i);
            if (row.stream().allMatch(String::isBlank)) continue;
            int fila=i+1;
            String dni=valor(row,indices,"dni").replaceAll("\\D","");
            String nombre=valor(row,indices,"nombre"), apellido=valor(row,indices,"apellido");
            if (!completo) {
                String full=valor(row,indices,"nombre y apellido").trim();
                String[] words=full.split("\\s+");
                // Nombre completo ambiguo: no asumir apellidos compuestos.
                if (words.length != 2) {
                    errores.add(new ErrorFila(fila,"Nombre compuesto ambiguo: separar nombre y apellido en plantilla completa"));
                    continue;
                }
                nombre=words[0]; apellido=words[1];
            }
            String nSocio=valor(row,indices,"numero_socio");
            if (nSocio.isBlank()) nSocio=valor(row,indices,"n° de socio");
            Integer numero=null;
            try {
                if(!nSocio.isBlank()) {numero=Integer.valueOf(nSocio);if(numero<1)throw new IllegalArgumentException();}
            } catch(Exception e) { errores.add(new ErrorFila(fila,"Número de socio inválido"));continue; }
            if(!dni.matches("\\d{7,10}") || nombre.length()<2 || apellido.length()<2 ||
                    nombre.length()>50 || apellido.length()>50) {
                errores.add(new ErrorFila(fila,"DNI, nombre o apellido inválido"));continue;
            }
            if(!dnis.add(dni) || (numero!=null && !numeros.add(numero))) {
                errores.add(new ErrorFila(fila,"DNI o número de socio repetido en archivo"));continue;
            }
            List<String> meses=new ArrayList<>();
            boolean invalido=false;
            for(String mes:MESES) {
                String raw=normalizar(valor(row,indices,mes));
                String estado=switch(raw) {
                    case "", "-" -> "";
                    case "pago", "pago realizado", "pagado", "si", "sí" -> "PAGADO";
                    case "no pago", "no pagado", "pendiente", "impago" -> "PENDIENTE";
                    default -> "INVALIDO";
                };
                if (estado.equals("INVALIDO")) {
                    errores.add(new ErrorFila(fila,"Estado desconocido en "+mes)); invalido=true;
                }
                meses.add(estado);
            }
            if(invalido) continue;
            LocalDate nacimiento=fecha(valor(row,indices,"fecha_nacimiento"),fila,"Nacimiento",errores);
            LocalDate alta=fecha(valor(row,indices,"fecha_alta"),fila,"Alta",errores);
            String estado=valor(row,indices,"estado");
            estado=estado.isBlank()?"ACTIVO":estado.toUpperCase(Locale.ROOT);
            if(!Set.of("ACTIVO","INACTIVO").contains(estado)) {
                errores.add(new ErrorFila(fila,"Estado de socio inválido"));continue;
            }
            String email=valor(row,indices,"email"), telefono=valor(row,indices,"telefono");
            String direccion=valor(row,indices,"direccion");
            if((!email.isBlank() && (!email.contains("@") || email.length()>120)) ||
                    telefono.length()>30 || direccion.length()>200) {
                errores.add(new ErrorFila(fila,"Contacto inválido"));continue;
            }
            filas.add(new Fila(fila,dni,nombre.trim(),apellido.trim(),numero,telefono,email,direccion,
                    estado,nacimiento,alta,meses));
            for(String mes:meses) {
                if(mes.equals("PAGADO"))pagados++;
                else if(mes.equals("PENDIENTE"))pendientes++;
                else vacios++;
            }
        }
        if(filas.isEmpty() && errores.isEmpty()) errores.add(new ErrorFila(0,"No hay socios para importar"));
        if(filas.size()>5000) errores.add(new ErrorFila(0,"Máximo 5000 socios"));
        Map<String,Socio> existentes=new HashMap<>();
        Map<Integer,String> numerosUsados=new HashMap<>();
        for(Socio s:socios.findByClubId(clubId)){
            existentes.put(s.getDni(),s);
            if(s.getNumeroSocio()!=null)numerosUsados.put(s.getNumeroSocio(),s.getDni());
        }
        int nuevos=0, yaExistentes=0, omitidos=0;
        for(Fila f:filas) {
            Socio registrado=existentes.get(f.dni());
            if(registrado==null)nuevos++;else yaExistentes++;
            if(f.numeroSocio()!=null && numerosUsados.containsKey(f.numeroSocio()) &&
                    !numerosUsados.get(f.numeroSocio()).equals(f.dni())) {
                errores.add(new ErrorFila(f.numero(),"Número de socio pertenece a otro DNI"));
            }
            if(registrado!=null && f.numeroSocio()!=null && registrado.getNumeroSocio()!=null &&
                    !f.numeroSocio().equals(registrado.getNumeroSocio())) {
                errores.add(new ErrorFila(f.numero(),"El número del socio existente no coincide"));
            }
            if(registrado!=null && (!registrado.getNombre().equalsIgnoreCase(f.nombre()) ||
                    !registrado.getApellido().equalsIgnoreCase(f.apellido()))) {
                errores.add(new ErrorFila(f.numero(),"Nombre distinto del socio registrado; requiere revisión"));
            }
            if(registrado!=null) for(int mes=0;mes<12;mes++) {
                if(f.meses().get(mes).isEmpty())continue;
                String periodo=String.format("%04d-%02d",anio,mes+1);
                List<String> estados=jdbc.queryForList(
                    "SELECT estado FROM historial_cuota_importada WHERE club_id=? AND socio_id=? AND periodo=?",
                    String.class,clubId,registrado.getId(),periodo);
                if(!estados.isEmpty()){
                    if(!estados.getFirst().equals(f.meses().get(mes)))
                        errores.add(new ErrorFila(f.numero(),"Historial contradictorio en "+periodo));
                    else omitidos++;
                }
            }
        }
        if(!errores.isEmpty())return new Resultado(false,false,nuevos,yaExistentes,pagados,pendientes,vacios,omitidos,errores);
        if(!confirmar)return new Resultado(true,false,nuevos,yaExistentes,pagados,pendientes,vacios,omitidos,List.of());

        int nextNumber=socios.maxNumeroSocioPorClub(clubId)+1;
        Set<Integer> reserved=new HashSet<>(numeros);
        reserved.addAll(numerosUsados.keySet());
        for(Fila f:filas) {
            Socio socio=existentes.get(f.dni());
            if(socio==null){
                socio=new Socio();
                socio.setClub(club);socio.setDni(f.dni());socio.setNombre(f.nombre());socio.setApellido(f.apellido());
                socio.setEstado(f.estado());
                socio.setTelefono(f.telefono().isBlank()?null:f.telefono());
                socio.setEmail(f.email().isBlank()?null:f.email());
                socio.setDireccion(f.direccion().isBlank()?null:f.direccion());
                socio.setFechaNacimiento(f.nacimiento());
                socio.setFechaAlta(f.alta()==null?LocalDate.now():f.alta());
                Integer numero=f.numeroSocio();
                if(numero==null){while(reserved.contains(nextNumber))nextNumber++;numero=nextNumber++;reserved.add(numero);}
                socio.setNumeroSocio(numero);
                socio=socios.saveAndFlush(socio);
            }
            for(int mes=0;mes<12;mes++) {
                String estado=f.meses().get(mes);
                if(estado.isBlank())continue;
                String periodo=String.format("%04d-%02d",anio,mes+1);
                // Idempotencia: sólo se insertan estados inexistentes. Nunca se altera el libro contable.
                jdbc.update("INSERT INTO historial_cuota_importada (club_id,socio_id,periodo,estado,fuente) "+
                    "SELECT ?,?,?,?,'CSV' WHERE NOT EXISTS ("+
                    "SELECT 1 FROM historial_cuota_importada WHERE club_id=? AND socio_id=? AND periodo=?)",
                    clubId,socio.getId(),periodo,estado,clubId,socio.getId(),periodo);
            }
        }
        auditoria.registrar(actor, clubId, "IMPORTACION_INTEGRAL", "HISTORIAL_CUOTAS", null,
                "Socios nuevos="+nuevos+"; existentes="+yaExistentes+"; pagados declarados="+pagados+
                "; pendientes declarados="+pendientes+"; año="+anio);
        return new Resultado(true,true,nuevos,yaExistentes,pagados,pendientes,vacios,omitidos,List.of());
    }

    public record Historial(long socioId, String dni, String socio, String periodo, String estado) {}

    @Transactional(readOnly = true)
    public List<Historial> listarHistorial(Long clubId) {
        if(!clubes.existsById(clubId)) throw new IllegalArgumentException("Institución inexistente");
        return jdbc.query(
            "SELECT s.id, s.dni, s.nombre, s.apellido, h.periodo, h.estado " +
            "FROM historial_cuota_importada h JOIN socio s ON s.id=h.socio_id AND s.club_id=h.club_id " +
            "WHERE h.club_id=? ORDER BY s.numero_socio, h.periodo",
            (rs, n) -> new Historial(rs.getLong(1),rs.getString(2),
                rs.getString(3)+" "+rs.getString(4),rs.getString(5),rs.getString(6)), clubId);
    }

    private static String valor(List<String> row,Map<String,Integer> idx,String key){
        Integer n=idx.get(key);
        return n==null||n>=row.size()?"":row.get(n).trim();
    }
    private static String normalizar(String s){
        String v=Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD);
        return v.replaceAll("\\p{M}","").replaceAll("\\s+"," ");
    }
    private static LocalDate fecha(String value,int row,String field,List<ErrorFila> errors){
        if(value.isBlank())return null;
        try {return LocalDate.parse(value);}
        catch(Exception ex){errors.add(new ErrorFila(row,"Fecha inválida "+field+" (YYYY-MM-DD)"));return null;}
    }

    static List<List<String>> parsear(String input){
        if(input==null||input.isBlank())return List.of();
        String csv=input.replace("\uFEFF","");
        char delimiter=',';
        // El control de planillas puede venir separado por punto y coma.
        String first=csv.lines().filter(s->!s.isBlank()).findFirst().orElse("");
        long commas=first.chars().filter(c->c==',').count();
        long semis=first.chars().filter(c->c==';').count();
        if(semis>commas)delimiter=';';
        List<List<String>> rows=new ArrayList<>();
        List<String> row=new ArrayList<>();
        StringBuilder cell=new StringBuilder();
        boolean quoted=false;
        for(int i=0;i<csv.length();i++){
            char c=csv.charAt(i);
            if(c=='"'){
                if(quoted && i+1<csv.length() && csv.charAt(i+1)=='"'){cell.append('"');i++;}
                else quoted=!quoted;
            }else if(c==delimiter && !quoted){
                row.add(cell.toString());cell.setLength(0);
            }else if((c=='\n'||c=='\r')&&!quoted){
                if(c=='\r'&&i+1<csv.length()&&csv.charAt(i+1)=='\n')i++;
                row.add(cell.toString());rows.add(row);row=new ArrayList<>();cell.setLength(0);
            }else cell.append(c);
        }
        if(quoted)throw new IllegalArgumentException("CSV con comillas sin cerrar");
        if(!row.isEmpty()||!cell.isEmpty()){row.add(cell.toString());rows.add(row);}
        return rows;
    }
}
