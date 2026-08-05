import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { CuotaVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class CuotaService {
  private readonly http = inject(HttpClient);
  listar(clubId: number, filtros?: { periodo?: string; estado?: string; socioId?: number }) {
    let params = new HttpParams();
    if (filtros?.periodo) params = params.set('periodo', filtros.periodo);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    if (filtros?.socioId) params = params.set('socioId', filtros.socioId);
    return this.http.get<CuotaVista[]>(`${API_BASE_URL}/clubes/${clubId}/cuotas`, { params });
  }
  crear(clubId: number, payload: object) { return this.http.post<CuotaVista>(`${API_BASE_URL}/clubes/${clubId}/cuotas`, payload); }
  generar(clubId: number, payload: object) { return this.http.post<{ creadas: number; omitidas: number }>(`${API_BASE_URL}/clubes/${clubId}/cuotas/generacion`, payload); }
}
