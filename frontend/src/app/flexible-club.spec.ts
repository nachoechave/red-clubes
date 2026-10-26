import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { AuthSessionStore } from './core/auth/auth-session.store';
import { ClubVista } from './core/models/gestion.models';
import { etiquetaParticipantes, seccionHabilitada } from './core/models/club-config';
import { ActividadService } from './features/actividades/actividad.service';
import { InscripcionService } from './features/inscripciones/inscripcion.service';

const futbol: ClubVista = { id: 1, nombre: 'Club barrial', direccion: 'Calle 1', estado: 'ACTIVO', tipoInstitucion: 'CLUB',
  modulos: ['PARTICIPANTES', 'CATEGORIAS'], nombreParticipantes: 'JUGADORES' };

describe('Configuración flexible', () => {
  it('oculta módulos deshabilitados incluso para rutas directas', () => {
    for (const ruta of ['cuotas', 'asistencias', 'talleres']) expect(seccionHabilitada(futbol, ruta)).toBe(false);
    for (const ruta of ['socios', 'socios/123', 'actividades', 'categorias', 'inscripciones']) expect(seccionHabilitada(futbol, ruta)).toBe(true);
  });
  it('conserva módulos históricos cuando falta configuración', () => {
    expect(seccionHabilitada(undefined, 'cuotas')).toBe(true);
    expect(etiquetaParticipantes(undefined)).toBe('Socios');
  });
  it('permite un club vacío sin reactivar módulos por defecto', () => {
    expect(seccionHabilitada({ ...futbol, modulos: [] }, 'socios')).toBe(false);
    expect(seccionHabilitada({ ...futbol, modulos: [] }, 'actividades')).toBe(false);
  });
  it('usa nombres contextuales sin tipos rígidos de club', () => {
    expect(etiquetaParticipantes(futbol)).toBe('Jugadores');
    expect(etiquetaParticipantes({ ...futbol, nombreParticipantes: 'ALUMNOS' })).toBe('Alumnos');
    expect(etiquetaParticipantes({ ...futbol, nombreParticipantes: 'PARTICIPANTES' })).toBe('Participantes');
  });
});

describe('Pantallas de grupos', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({ imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
  });
  function preparar() {
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(App);
    // Acceso al estado del componente para verificar el formulario real y la navegación.
    const app = fixture.componentInstance as any;
    TestBed.inject(AuthSessionStore).setUser({ id: 1, dni: '12345678', nombre: 'Admin', apellido: 'Prueba',
      rol: 'SUPERUSUARIO', estado: 'ACTIVO', debeCambiarPassword: false, clubes: [] });
    app.clubes.set([futbol]); app.clubActivoId.set(1); app.activeSection.set('actividades');
    app.nuevaActividad.tipo = 'CATEGORIA_DEPORTIVA';
    app.nuevaActividad.asistenciaHabilitada = false;
    fixture.detectChanges();
    return { fixture, app, html: fixture.nativeElement as HTMLElement };
  }
  it('categoría no muestra profesor, días, icono ni cupo obligatorios', () => {
    const { html } = preparar();
    for (const name of ['actividadProfesor', 'actividadDia', 'actividadHora', 'actividadIcono', 'actividadCupo', 'actividadAsistencia']) {
      expect(html.querySelector(`[name="${name}"]`)).toBeNull();
    }
    expect(html.querySelector('[name="actividadDisciplina"]')).not.toBeNull();
    expect(html.querySelector('[name="actividadAnio"]')).not.toBeNull();
  });
  it('menú muestra Jugadores y Categorías y oculta asistencia y cuotas', () => {
    const { app } = preparar();
    const items = app.navItems();
    expect(items.find((item: any) => item.id === 'socios').label).toBe('Jugadores');
    expect(items.find((item: any) => item.id === 'actividades').label).toBe('Categorías');
    expect(items.some((item: any) => ['asistencias', 'cuotas'].includes(item.id))).toBe(false);
  });
  it('redirige accesos directos a módulos deshabilitados', () => {
    const { app } = preparar();
    const spy = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    app.goTo('cuotas');
    expect(spy).toHaveBeenCalledWith('/configuracion');
  });
  it('envía categoría sin profesor ni cupo a la API genérica', () => {
    const { app } = preparar();
    app.nuevaActividad.nombre = 'Categoría 2014';
    app.crearActividad();
    const req = TestBed.inject(HttpTestingController).expectOne('/api/clubes/1/grupos');
    expect(req.request.body.tipo).toBe('CATEGORIA_DEPORTIVA');
    expect(req.request.body.profesorUsuarioId).toBeNull();
    expect(req.request.body.cupo).toBeNull();
    expect(req.request.body.dias).toBe('');
    expect(req.request.body.asistenciaHabilitada).toBe(false);
    req.flush({ message: 'prueba' }, { status: 400, statusText: 'Bad Request' });
  });
  it('un club mixto ofrece los tres tipos y formularios de taller', () => {
    const { fixture, app, html } = preparar();
    app.clubes.set([{ ...futbol, modulos: ['PARTICIPANTES','CATEGORIAS','ACTIVIDADES','TALLERES','PROFESORES','ASISTENCIAS'] }]);
    app.nuevaActividad.tipo = 'TALLER'; fixture.detectChanges();
    expect(app.tiposGrupo()).toHaveLength(3);
    expect(html.querySelector('[name="actividadProfesor"]')).not.toBeNull();
    expect(html.querySelector('[name="actividadDia"]')).not.toBeNull();
    expect(html.querySelector('[name="actividadAsistencia"]')).not.toBeNull();
    expect(html.querySelector('[name="actividadDisciplina"]')).toBeNull();
  });
  it('filtra grupos y nunca habilita asistencia por error en categorías', () => {
    const { app } = preparar();
    const categoria = { id: 1, tipo: 'CATEGORIA_DEPORTIVA', asistenciaHabilitada: false };
    app.actividades.set([categoria, { id: 2, tipo: 'ACTIVIDAD', asistenciaHabilitada: true }]);
    expect(app.gruposVisibles()).toEqual([categoria]);
    expect(app.asistenciaDisponible(categoria)).toBe(false);
  });
});

describe('API de grupos', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  it('lista todos los tipos por club', () => {
    TestBed.inject(ActividadService).listarGrupos(7).subscribe();
    TestBed.inject(HttpTestingController).expectOne('/api/clubes/7/grupos').flush([]);
  });
  it('reemplaza inscripciones de todos los tipos en una sola operación', () => {
    TestBed.inject(InscripcionService).actualizarGrupos(7, 4, [1, 2, 3]).subscribe();
    const req = TestBed.inject(HttpTestingController).expectOne('/api/clubes/7/socios/4/grupos');
    expect(req.request.body.grupoIds).toEqual([1, 2, 3]); req.flush([]);
  });
});
