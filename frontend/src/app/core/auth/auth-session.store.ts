import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UsuarioApp } from './auth.models';

const SESSION_TOKEN_KEY = 'red-clubes.session-token';

@Injectable({ providedIn: 'root' })
export class AuthSessionStore {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly browser = isPlatformBrowser(this.platformId);

  readonly token = signal(this.readToken());
  readonly usuario = signal<UsuarioApp | null>(null);

  setSession(token: string, usuario: UsuarioApp): void {
    this.token.set(token);
    this.usuario.set(usuario);
    if (this.browser) {
      sessionStorage.setItem(SESSION_TOKEN_KEY, token);
    }
  }

  setUser(usuario: UsuarioApp): void {
    this.usuario.set(usuario);
  }

  clear(): void {
    this.token.set('');
    this.usuario.set(null);
    if (this.browser) {
      sessionStorage.removeItem(SESSION_TOKEN_KEY);
    }
  }

  private readToken(): string {
    return this.browser ? sessionStorage.getItem(SESSION_TOKEN_KEY) ?? '' : '';
  }
}
