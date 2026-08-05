import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { SocioVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class SocioService {
  private readonly http = inject(HttpClient);
  listar(clubId: number) { return this.http.get<SocioVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios`); }
  crear(clubId: number, payload: Partial<SocioVista>) { return this.http.post<SocioVista>(`${API_BASE_URL}/clubes/${clubId}/socios`, payload); }
  actualizar(clubId: number, id: number, payload: Partial<SocioVista>) { return this.http.put<SocioVista>(`${API_BASE_URL}/clubes/${clubId}/socios/${id}`, payload); }
  desactivar(clubId: number, id: number, payload: Partial<SocioVista>) { return this.actualizar(clubId, id, payload); }
}
