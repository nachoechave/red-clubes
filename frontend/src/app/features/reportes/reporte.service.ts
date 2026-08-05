import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ReportesData } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);
  cargar(clubId: number, anio: number, filtros?: { desde?: string; hasta?: string; actividadId?: number; estadoSocio?: string }) {
    let params = new HttpParams().set('anio', anio);
    if (filtros?.desde) params = params.set('desde', filtros.desde);
    if (filtros?.hasta) params = params.set('hasta', filtros.hasta);
    if (filtros?.actividadId) params = params.set('actividadId', filtros.actividadId);
    if (filtros?.estadoSocio) params = params.set('estadoSocio', filtros.estadoSocio);
    return this.http.get<ReportesData>(`${API_BASE_URL}/clubes/${clubId}/reportes`, { params });
  }
}
