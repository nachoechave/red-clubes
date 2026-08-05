import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { UsuarioApp } from './auth.models';
import { AuthSessionStore } from './auth-session.store';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let session: AuthSessionStore;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    session = TestBed.inject(AuthSessionStore);
    router = TestBed.inject(Router);
  });

  afterEach(() => sessionStorage.clear());

  it('redirects an anonymous visitor to login', () => {
    const result = runGuard('dashboard') as UrlTree;
    expect(router.serializeUrl(result)).toBe('/login');
  });

  it('limits a professor to operational sections', () => {
    const professor: UsuarioApp = {
      id: 1,
      dni: '12345678',
      nombre: 'Ada',
      apellido: 'Lovelace',
      rol: 'PROFESOR',
      estado: 'ACTIVO',
      debeCambiarPassword: false,
      clubes: [],
    };
    session.setSession('opaque-token', professor);

    const denied = runGuard('cuotas') as UrlTree;
    expect(router.serializeUrl(denied)).toBe('/actividades');
    expect(runGuard('asistencias')).toBe(true);
  });

  function runGuard(path: string) {
    const route = { routeConfig: { path } } as ActivatedRouteSnapshot;
    const state = {} as RouterStateSnapshot;
    return TestBed.runInInjectionContext(() => authGuard(route, state));
  }
});
