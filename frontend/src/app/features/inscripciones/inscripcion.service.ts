import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ActividadVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class InscripcionService {
  private readonly http = inject(HttpClient);
  actualizarGrupos(clubId: number, socioId: number, grupoIds: number[], observaciones?: string | null) {
    return this.http.put<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/grupos`, { grupoIds, observaciones });
  }
  actualizar(clubId: number, socioId: number, actividadIds: number[], observaciones?: string | null) {
    return this.http.put<ActividadVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios/${socioId}/actividades`, { actividadIds, observaciones });
  }
}
