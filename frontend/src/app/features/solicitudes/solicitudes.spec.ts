import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SolicitudesComponent } from './solicitudes';

describe('Bandeja de solicitudes', () => {
  beforeEach(async () => { await TestBed.configureTestingModule({ imports: [SolicitudesComponent], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents(); });
  it('descarta una lista anterior al cambiar de institución', () => {
    const fixture = TestBed.createComponent(SolicitudesComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('clubId', 1); fixture.detectChanges();
    const anterior = http.expectOne('/api/clubes/1/solicitudes?estado=PENDIENTE&pagina=0');
    fixture.componentRef.setInput('clubId', 2); fixture.detectChanges();
    http.expectOne('/api/clubes/2/solicitudes?estado=PENDIENTE&pagina=0').flush({ contenido: [], pagina: 0, totalPaginas: 0, total: 0 });
    anterior.flush({ contenido: [{id: 1, datos: {nombre:'No mostrar',apellido:'Otra institución',dni:'99111222'},fechaCreacion:'2026-10-23T10:00:00',estado:'PENDIENTE'}],pagina:0,totalPaginas:1,total:1 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('No mostrar'); http.verify();
  });
  it('envía datos corregidos una sola vez mientras se resuelve la aprobación', () => {
    const fixture = TestBed.createComponent(SolicitudesComponent);
    const http = TestBed.inject(HttpTestingController); const app = fixture.componentInstance as any;
    fixture.componentRef.setInput('clubId', 1); fixture.detectChanges();
    http.expectOne('/api/clubes/1/solicitudes?estado=PENDIENTE&pagina=0').flush({contenido:[],pagina:0,totalPaginas:0,total:0});
    app.revisar({id:12,datos:{nombre:'Ana',apellido:'Prueba',dni:'99111222',email:'prueba@example.invalid',telefono:'2215551234'},estado:'PENDIENTE'});
    app.datos.nombre='Ana María'; app.resolver('aprobar'); app.resolver('aprobar');
    const request=http.expectOne('/api/clubes/1/solicitudes/12/aprobar'); expect(request.request.body.nombre).toBe('Ana María');
    request.flush({estado:'APROBADA'});
    http.expectOne('/api/clubes/1/solicitudes?estado=PENDIENTE&pagina=0').flush({contenido:[],pagina:0,totalPaginas:0,total:0});
    expect(app.mensaje()).toContain('dado de alta'); http.verify();
  });
});
