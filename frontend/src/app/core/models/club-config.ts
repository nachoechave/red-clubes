import { ClubVista, ModuloClub, TipoGrupo } from './gestion.models';

export const MODULOS_LEGACY: ModuloClub[] = ['PARTICIPANTES', 'ACTIVIDADES', 'PROFESORES', 'ASISTENCIAS', 'CUOTAS'];
export const TIPOS_GRUPO: { tipo: TipoGrupo; modulo: ModuloClub; nombre: string }[] = [
  { tipo: 'CATEGORIA_DEPORTIVA', modulo: 'CATEGORIAS', nombre: 'Categoría deportiva' },
  { tipo: 'ACTIVIDAD', modulo: 'ACTIVIDADES', nombre: 'Actividad' },
  { tipo: 'TALLER', modulo: 'TALLERES', nombre: 'Taller' },
];
export function moduloHabilitado(club: ClubVista | undefined, modulo: ModuloClub): boolean {
  return (club?.modulos ?? MODULOS_LEGACY).includes(modulo);
}
export function seccionHabilitada(club: ClubVista | undefined, seccion: string): boolean {
  const grupos = TIPOS_GRUPO.some(tipo => moduloHabilitado(club, tipo.modulo));
  switch (seccion.split('/')[0]) {
    case 'socios': case 'nuevo-socio': return moduloHabilitado(club, 'PARTICIPANTES');
    case 'categorias': return moduloHabilitado(club, 'CATEGORIAS');
    case 'talleres': return moduloHabilitado(club, 'TALLERES');
    case 'actividades': return grupos;
    case 'inscripciones': return grupos && moduloHabilitado(club, 'PARTICIPANTES');
    case 'asistencias': return grupos && moduloHabilitado(club, 'ASISTENCIAS');
    case 'cuotas': return moduloHabilitado(club, 'CUOTAS');
    default: return true;
  }
}
export function etiquetaParticipantes(club: ClubVista | undefined): string {
  const etiquetas = { SOCIOS: 'Socios', JUGADORES: 'Jugadores', ALUMNOS: 'Alumnos', PARTICIPANTES: 'Participantes' };
  return etiquetas[club?.nombreParticipantes ?? 'SOCIOS'];
}
