import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';

export interface ClubConsulta {
  id: number;
  nombre: string;
  sociosTotales: number;
  sociosActivos: number;
}

export interface SocioConsulta {
  id: number;
  clubId: number;
  nombre: string;
  apellido: string;
  dni: string;
  numeroSocio: number | null;
  estado: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  fechaNacimiento: string | null;
  fechaAlta: string | null;
  mesesPagadosHistoricos: number;
  mesesPendientesHistoricos: number;
}

@Injectable({ providedIn: 'root' })
export class ConsultaGlobalService {
  private readonly http = inject(HttpClient);
  listarClubes() { return this.http.get<ClubConsulta[]>(`${API_BASE_URL}/consulta-global/clubes`); }
  listarSocios(clubId?: number) {
    const url = `${API_BASE_URL}/consulta-global/socios`;
    return this.http.get<SocioConsulta[]>(clubId ? `${url}?clubId=${clubId}` : url);
  }
}
