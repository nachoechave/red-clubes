import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SocioService } from './socios/socio.service';
import { PagoService } from './cuotas/pago.service';
import { CuotaService } from './cuotas/cuota.service';
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
    TestBed.inject(PagoService).registrar(7, 22, 2500, 'TRANSFERENCIA').subscribe();
    const request = http.expectOne('/api/clubes/7/cuotas/22/pagos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ importe: 2500, medioPago: 'TRANSFERENCIA' });
    request.flush({});
  });

  it('uploads historical debt as multipart CSV', () => {
    const archivo = new File(['dni;periodo;importe;vencimiento'], 'deuda.csv', { type: 'text/csv' });
    TestBed.inject(CuotaService).importarHistorica(7, archivo).subscribe();
    const request = http.expectOne('/api/clubes/7/cuotas/importacion-historica');
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBe(true);
    expect((request.request.body as FormData).get('archivo')).toBe(archivo);
    request.flush({ valida: true, totalFilas: 0, importadas: 0, cuotasImportadas: [], errores: [] });
  });

  it('encodes the selected dashboard period', () => {
    TestBed.inject(DashboardService).cargar(7, '2026-08').subscribe();
    http.expectOne('/api/clubes/7/dashboard?periodo=2026-08').flush({});
  });
});
