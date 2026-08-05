import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthSessionStore } from './auth-session.store';
import { authInterceptor } from './auth.interceptor';
import { ApiErrorStore } from '../errors/api-error.store';

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;
  let session: AuthSessionStore;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
    session = TestBed.inject(AuthSessionStore);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('adds the opaque bearer only to API requests', () => {
    session.token.set('opaque-token');
    const http = TestBed.inject(HttpClient);

    http.get('/api/clubes').subscribe();
    const apiRequest = httpTesting.expectOne('/api/clubes');
    expect(apiRequest.request.headers.get('Authorization')).toBe('Bearer opaque-token');
    apiRequest.flush([]);

    http.get('https://example.test/public').subscribe();
    const externalRequest = httpTesting.expectOne('https://example.test/public');
    expect(externalRequest.request.headers.has('Authorization')).toBe(false);
    externalRequest.flush({});
  });

  it('clears the local session after an unauthorized API response', () => {
    session.token.set('expired-token');
    const http = TestBed.inject(HttpClient);

    http.get('/api/clubes').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/clubes').flush(
      { message: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(session.token()).toBe('');
    expect(session.usuario()).toBeNull();
  });

  it('publishes a centralized message after a forbidden API response', () => {
    const http = TestBed.inject(HttpClient);
    const errors = TestBed.inject(ApiErrorStore);
    http.get('/api/usuarios').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/usuarios').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(errors.message()).toContain('permisos');
  });
});
