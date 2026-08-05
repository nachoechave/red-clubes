import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { AsistenciaVista, EstadoAsistencia } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private readonly http = inject(HttpClient);
  listar(clubId: number, actividadId: number, fecha: string) { return this.http.get<AsistenciaVista[]>(`${API_BASE_URL}/clubes/${clubId}/actividades/${actividadId}/asistencias/${fecha}`); }
  guardar(clubId: number, actividadId: number, fecha: string, payload: { socioId: number; estado: EstadoAsistencia }[]) {
    return this.http.post<AsistenciaVista[]>(`${API_BASE_URL}/clubes/${clubId}/actividades/${actividadId}/asistencias/${fecha}`, payload);
  }
}
