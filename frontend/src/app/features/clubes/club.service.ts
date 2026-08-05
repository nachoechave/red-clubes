import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { ClubVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class ClubService {
  private readonly http = inject(HttpClient);
  listar() { return this.http.get<ClubVista[]>(`${API_BASE_URL}/clubes`); }
  crear(payload: Omit<ClubVista, 'id'>) { return this.http.post<ClubVista>(`${API_BASE_URL}/clubes`, payload); }
  actualizar(id: number, payload: Omit<ClubVista, 'id'>) { return this.http.put<ClubVista>(`${API_BASE_URL}/clubes/${id}`, payload); }
}
