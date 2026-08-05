import { TestBed } from '@angular/core/testing';
import { AuthSessionStore } from './auth-session.store';
import { UsuarioApp } from './auth.models';

const usuario: UsuarioApp = {
  id: 1,
  dni: '12345678',
  nombre: 'Ada',
  apellido: 'Lovelace',
  rol: 'ADMINISTRADOR',
  estado: 'ACTIVO',
  debeCambiarPassword: false,
  clubes: [],
};

describe('AuthSessionStore', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
  });

  afterEach(() => sessionStorage.clear());

  it('persists the bearer for a page reload in the current tab', () => {
    const store = TestBed.inject(AuthSessionStore);

    store.setSession('opaque-token', usuario);

    expect(store.token()).toBe('opaque-token');
    expect(store.usuario()).toEqual(usuario);
    expect(sessionStorage.getItem('red-clubes.session-token')).toBe('opaque-token');
  });

  it('clears memory and browser session together', () => {
    const store = TestBed.inject(AuthSessionStore);
    store.setSession('opaque-token', usuario);

    store.clear();

    expect(store.token()).toBe('');
    expect(store.usuario()).toBeNull();
    expect(sessionStorage.getItem('red-clubes.session-token')).toBeNull();
  });
});
