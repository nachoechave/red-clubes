import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { DashboardData } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  cargar(clubId: number, periodo: string) { return this.http.get<DashboardData>(`${API_BASE_URL}/clubes/${clubId}/dashboard?periodo=${encodeURIComponent(periodo)}`); }
}
