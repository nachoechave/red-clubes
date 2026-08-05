import { RolClub } from '../auth/auth.models';

export type EstadoSocio = 'ACTIVO' | 'INACTIVO';
export type EstadoCuota = 'PENDIENTE' | 'PAGADA' | 'VENCIDA' | 'ANULADA';
export type EstadoClub = 'ACTIVO' | 'INACTIVO';
export type EstadoActividad = 'ACTIVA' | 'INACTIVA';
export type EstadoAsistencia = 'PRESENTE' | 'AUSENTE' | 'JUSTIFICADO';
export type MedioPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA' | 'OTRO';

export interface ClubVista {
  id: number;
  nombre: string;
  direccion: string;
  logoUrl?: string | null;
  estado: EstadoClub;
}

export interface AsignacionUsuarioForm {
  clubId: number | null;
  rolClub: RolClub;
  actividadIds: number[];
}

export interface SocioVista {
  id: number;
  clubId: number;
  nombre: string;
  apellido: string;
  dni: string;
  email: string;
  fechaNacimiento?: string | null;
  fechaAlta?: string | null;
  numeroSocio?: number | null;
  telefono: string;
  direccion: string;
  emergenciaNombre: string;
  emergenciaTelefono: string;
  emergenciaRelacion: string;
  estado: EstadoSocio;
}

export interface ActividadVista {
  id: number;
  clubId: number;
  clubNombre: string;
  nombre: string;
  profesor: string;
  profesorUsuarioId?: number | null;
  dias: string;
  categoria: string;
  icono: string;
  cupo: number;
  inscriptos: number;
  estado: EstadoActividad;
}

export interface CuotaVista {
  id: number;
  clubId: number;
  socioId: number;
  socioNombre: string;
  socioDni: string;
  mes: string;
  periodo?: string;
  importe: number;
  estado: EstadoCuota;
  vencimiento: string;
}

export interface PagoVista {
  id: number;
  cuotaId: number;
  importe: number;
  fechaPago?: string | null;
  medioPago: MedioPago | 'MIGRACION';
  usuarioResponsableId?: number | null;
  observaciones?: string | null;
  estado: 'ACTIVO' | 'ANULADO';
  fechaAnulacion?: string | null;
}

export interface CuotaResumenSocio {
  socioId: number;
  socioNombre: string;
  socioDni: string;
  estado: 'AL_DIA' | 'NO_AL_DIA';
  pendientes: number;
  ultimoMes: string;
}

export interface AsistenciaVista {
  id: number | null;
  actividadId: number;
  socioId: number;
  socioNombre: string;
  socioDni: string;
  socioTelefono: string;
  fecha: string;
  presente: boolean;
  estado: EstadoAsistencia;
  usuarioResponsableId?: number | null;
}

export interface DashboardData {
  sociosActivos: number;
  sociosTotales: number;
  cuotasAlDia: number;
  morosos: number;
  actividadesActivas: number;
  asistenciaMes: number;
  totalCobrado: number;
  movimientos: { descripcion: string; momento: string; tono: string }[];
  proximasActividades: ActividadVista[];
  cuotasPendientes: CuotaVista[];
  sociosRecientes: { id: number; nombre: string; telefono: string; estado: string }[];
  pagosPorMes: { mes: string; valor: number }[];
  asistenciaMensual: { mes: string; valor: number }[];
}

export interface ReportesData {
  sociosPorClub: { club: string; socios: number; actividades: number }[];
  pagosPorMes: { mes: string; valor: number }[];
  asistenciaMensual: { mes: string; valor: number }[];
  deudaPendiente: number;
  deudaVencida: number;
  cuotasConDeuda: number;
  sociosConDeuda: number;
}
