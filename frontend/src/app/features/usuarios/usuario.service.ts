import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { UsuarioApp } from '../../core/auth/auth.models';

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  listar() { return this.http.get<UsuarioApp[]>(`${API_BASE_URL}/usuarios`); }
  crear(payload: object) { return this.http.post<UsuarioApp>(`${API_BASE_URL}/usuarios`, payload); }
  desactivar(id: number) { return this.http.delete<UsuarioApp>(`${API_BASE_URL}/usuarios/${id}`); }
  activar(id: number) { return this.http.put<UsuarioApp>(`${API_BASE_URL}/usuarios/${id}/activar`, {}); }
  actualizarAsignaciones(id: number, asignaciones: object[]) { return this.http.put<UsuarioApp>(`${API_BASE_URL}/usuarios/${id}/asignaciones`, { asignaciones }); }
}
