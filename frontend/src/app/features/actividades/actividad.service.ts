import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ActividadVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class ActividadService {
  private readonly http = inject(HttpClient);
  listar(clubId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/actividades`); }
  listarDeSocio(clubId: number, socioId: number) { return this.http.get<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/actividades`); }
  crear(clubId: number, payload: object) { return this.http.post<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/actividades`, payload); }
  actualizar(clubId: number, id: number, payload: object) { return this.http.put<ActividadVista>(`${API_BASE_URL}/clubes/${clubId}/actividades/${id}`, payload); }
}
