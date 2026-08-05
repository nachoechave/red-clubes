import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
const emptyRoute = () => import('./core/routing/empty-route.component').then((module) => module.EmptyRouteComponent);

const protectedSections = [
  'dashboard',
  'socios',
  'socios/nuevo',
  'socios/:id',
  'clubes',
  'actividades',
  'actividades/:id',
  'inscripciones',
  'cuotas',
  'asistencias',
  'reportes',
  'usuarios',
  'configuracion',
];

export const routes: Routes = [
  { path: 'login', loadComponent: emptyRoute, canActivate: [guestGuard] },
  ...protectedSections.map((path) => ({ path, loadComponent: emptyRoute, canActivate: [authGuard] })),
  { path: 'nuevo-socio', redirectTo: 'socios/nuevo' },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'no-encontrado', loadComponent: emptyRoute },
  { path: '**', redirectTo: 'no-encontrado' },
];
