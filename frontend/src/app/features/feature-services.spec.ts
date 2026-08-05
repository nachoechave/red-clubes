import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SocioService } from './socios/socio.service';
import { PagoService } from './cuotas/pago.service';
import { DashboardService } from './dashboard/dashboard.service';

describe('feature API services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('scopes member requests by club', () => {
    TestBed.inject(SocioService).listar(7).subscribe();
    http.expectOne('/api/clubes/7/socios').flush([]);
  });

  it('registers a payment below its club and fee', () => {
    TestBed.inject(PagoService).registrar(7, 22, 'TRANSFERENCIA').subscribe();
    const request = http.expectOne('/api/clubes/7/cuotas/22/pagos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ medioPago: 'TRANSFERENCIA' });
    request.flush({});
  });

  it('encodes the selected dashboard period', () => {
    TestBed.inject(DashboardService).cargar(7, '2026-08').subscribe();
    http.expectOne('/api/clubes/7/dashboard?periodo=2026-08').flush({});
  });
});
