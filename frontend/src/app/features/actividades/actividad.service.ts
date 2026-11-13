import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ActividadVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class ActividadService {
  private readonly http = inject(HttpClient);
  listarGrupos(clubId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/grupos`); }
  gruposDeSocio(clubId: number, socioId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/grupos`); }
  crearGrupo(clubId: number, payload: object) { return this.http.post<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/grupos`, payload); }
  actualizarGrupo(clubId: number, id: number, payload: object) { return this.http.put<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/grupos/${id}`, payload); }
  listar(clubId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/actividades`); }
  listarDeSocio(clubId: number, socioId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/actividades`); }
  crear(clubId: number, payload: object) { return this.http.post<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/actividades`, payload); }
  actualizar(clubId: number, id: number, payload: object) { return this.http.put<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/actividades/${id}`, payload); }
}
