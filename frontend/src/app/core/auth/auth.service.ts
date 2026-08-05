import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, of, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { LoginRequest, LoginResponse, UsuarioApp } from './auth.models';
import { AuthSessionStore } from './auth-session.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(AuthSessionStore);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap((response) => this.session.setSession(response.token, response.usuario)),
    );
  }

  restore(): Observable<UsuarioApp | null> {
    if (!this.session.token()) {
      return of(null);
    }

    return this.http.get<UsuarioApp>(`${API_BASE_URL}/auth/me`).pipe(
      tap((usuario) => this.session.setUser(usuario)),
      catchError(() => {
        this.session.clear();
        return of(null);
      }),
    );
  }

  logout(): Observable<void> {
    if (!this.session.token()) {
      this.session.clear();
      return of(undefined);
    }

    return this.http.post<void>(`${API_BASE_URL}/auth/logout`, {}).pipe(
      finalize(() => this.session.clear()),
    );
  }

  changePassword(passwordActual: string, nuevaPassword: string): Observable<UsuarioApp> {
    return this.http.post<UsuarioApp>(`${API_BASE_URL}/auth/cambiar-password`, { passwordActual, nuevaPassword });
  }

  updateProfile(nombre: string, apellido: string): Observable<UsuarioApp> {
    return this.http.put<UsuarioApp>(`${API_BASE_URL}/auth/me`, { nombre, apellido });
  }
}
