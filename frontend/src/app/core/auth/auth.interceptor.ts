import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { AuthSessionStore } from './auth-session.store';
import { ApiErrorStore } from '../errors/api-error.store';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthSessionStore);
  const apiErrors = inject(ApiErrorStore);
  const token = session.token();
  const isApiRequest = request.url === API_BASE_URL || request.url.startsWith(`${API_BASE_URL}/`);
  const authenticatedRequest = isApiRequest && token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.endsWith('/auth/login')) {
        session.clear();
      }
      if (isApiRequest && error.status === 403) {
        apiErrors.report('No tenes permisos para realizar esta operacion.');
      } else if (isApiRequest && error.status === 0) {
        apiErrors.report('No se pudo conectar con el servidor.');
      } else if (isApiRequest && error.status >= 500) {
        apiErrors.report('El servidor no pudo completar la operacion. Intenta nuevamente.');
      }
      return throwError(() => error);
    }),
  );
};
