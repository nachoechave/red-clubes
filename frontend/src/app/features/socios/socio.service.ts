import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ImportacionSociosVista, SocioVista } from '../../core/models/gestion.models';

export interface HistorialCuotaImportada {
  socioId: number;
  dni: string;
  socio: string;
  periodo: string;
  estado: 'PAGADO' | 'PENDIENTE';
}

export interface ImportacionIntegralResultado {
  valida: boolean;
  ejecutada: boolean;
  sociosNuevos: number;
  sociosExistentes: number;
  mesesPagados: number;
  mesesPendientes: number;
  mesesVacios: number;
  mesesOmitidos: number;
  errores: Array<{ fila: number; mensaje: string }>;
}

@Injectable({ providedIn: 'root' })
export class SocioService {
  private readonly http = inject(HttpClient);
  listar(clubId: number) { return this.http.get<SocioVista[]>(`${API_BASE_URL}/clubes/${clubId}/socios`); }
  importar(clubId: number, archivo: File) {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<ImportacionSociosVista>(`${API_BASE_URL}/clubes/${clubId}/socios/importacion`, formData);
  }
  listarHistorialIntegral(clubId: number) {
    return this.http.get<HistorialCuotaImportada[]>(
      `${API_BASE_URL}/clubes/${clubId}/socios/importacion-integral/historial`);
  }
  importarIntegral(clubId: number, archivo: File, anio: number, confirmar = false) {
    const formData = new FormData();
    formData.append('archivo', archivo);
    formData.append('anio', String(anio));
    const accion = confirmar ? 'confirmar' : 'vista-previa';
    return this.http.post<ImportacionIntegralResultado>(
      `${API_BASE_URL}/clubes/${clubId}/socios/importacion-integral/${accion}`, formData);
  }
  crear(clubId: number, payload: Partial<SocioVista>) { return this.http.post<SocioVista>(`${API_BASE_URL}/clubes/${clubId}/socios`, payload); }
  actualizar(clubId: number, id: number, payload: Partial<SocioVista>) { return this.http.put<SocioVista>(`${API_BASE_URL}/clubes/${clubId}/socios/${id}`, payload); }
  desactivar(clubId: number, id: number, payload: Partial<SocioVista>) { return this.actualizar(clubId, id, payload); }
}
