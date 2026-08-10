import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSessionStore } from './auth-session.store';

const SECCIONES_PROFESOR = new Set(['actividades', 'asistencias', 'configuracion']);
const SECCIONES_OPERADOR = new Set([
  'dashboard', 'socios', 'socios/nuevo', 'actividades', 'inscripciones',
  'cuotas', 'asistencias', 'reportes', 'configuracion',
]);

function professorCanAccess(section: string): boolean {
  return SECCIONES_PROFESOR.has(section) || section.startsWith('actividades/');
}

function operatorCanAccess(section: string): boolean {
  return SECCIONES_OPERADOR.has(section) || section.startsWith('socios/') || section.startsWith('actividades/');
}

export const authGuard: CanActivateFn = (route) => {
  const session = inject(AuthSessionStore);
  const router = inject(Router);
  if (!session.token()) {
    return router.createUrlTree(['/login']);
  }

  const usuario = session.usuario();
  const seccion = route.routeConfig?.path ?? '';
  if (usuario?.rol === 'PROFESOR' && !professorCanAccess(seccion)) {
    return router.createUrlTree(['/actividades']);
  }
  if (usuario?.rol === 'OPERADOR' && !operatorCanAccess(seccion)) {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};

export const guestGuard: CanActivateFn = () => {
  const session = inject(AuthSessionStore);
  const router = inject(Router);
  return session.token() ? router.createUrlTree(['/dashboard']) : true;
};
