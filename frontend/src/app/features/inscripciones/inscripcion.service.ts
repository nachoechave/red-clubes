import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ActividadVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class InscripcionService {
  private readonly http = inject(HttpClient);
  actualizar(clubId: number, socioId: number, actividadIds: number[], observaciones?: string | null) {
    return this.http.put<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/actividades`, { actividadIds, observaciones });
  }
}
