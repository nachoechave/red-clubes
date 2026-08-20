import { CommonModule } from '@angular/common';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { AuthSessionStore } from './core/auth/auth-session.store';
import { AuthService } from './core/auth/auth.service';
import { ClubAsignado, RolClub, RolUsuario, UsuarioApp } from './core/auth/auth.models';
import { ActividadVista, AsignacionUsuarioForm, AsistenciaVista, ClubVista, CuotaResumenSocio, CuotaVista, DashboardData, EstadoActividad, EstadoAsistencia, EstadoSocio, MedioPago, PagoVista, ReportesData, SocioVista } from './core/models/gestion.models';
import { ClubService } from './features/clubes/club.service';
import { SocioService } from './features/socios/socio.service';
import { ActividadService } from './features/actividades/actividad.service';
import { InscripcionService } from './features/inscripciones/inscripcion.service';
import { CuotaService } from './features/cuotas/cuota.service';
import { PagoService } from './features/cuotas/pago.service';
import { AsistenciaService } from './features/asistencias/asistencia.service';
import { DashboardService } from './features/dashboard/dashboard.service';
import { ReporteService } from './features/reportes/reporte.service';
import { UsuarioService } from './features/usuarios/usuario.service';
import { ApiErrorStore } from './core/errors/api-error.store';
import {
  LucideCalendarDays,
  LucideChartNoAxesColumnIncreasing,
  LucideCirclePercent,
  LucideClipboardCheck,
  LucideDumbbell,
  LucideHeartHandshake,
  LucideHeartPulse,
  LucideHouse,
  LucideKey,
  LucideMusic,
  LucidePalette,
  LucideSettings,
  LucideTheater,
  LucideUserRoundCheck,
  LucideUserPlus,
  LucideUserX,
  LucideUsers,
  LucideWalletCards,
} from '@lucide/angular';

const lucideIcons = [
  LucideCalendarDays,
  LucideChartNoAxesColumnIncreasing,
  LucideCirclePercent,
  LucideClipboardCheck,
  LucideDumbbell,
  LucideHeartHandshake,
  LucideHeartPulse,
  LucideHouse,
  LucideKey,
  LucideMusic,
  LucidePalette,
  LucideSettings,
  LucideTheater,
  LucideUserPlus,
  LucideUserRoundCheck,
  LucideUserX,
  LucideUsers,
  LucideWalletCards,
];

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterOutlet,
    ...lucideIcons,
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css', './app-legacy-features.css', './app-redesign.css']
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly sessionStore = inject(AuthSessionStore);
  private readonly router = inject(Router);
  private readonly clubService = inject(ClubService);
  private readonly socioService = inject(SocioService);
  private readonly actividadService = inject(ActividadService);
  private readonly inscripcionService = inject(InscripcionService);
  private readonly cuotaService = inject(CuotaService);
  private readonly pagoService = inject(PagoService);
  private readonly asistenciaService = inject(AsistenciaService);
  private readonly dashboardService = inject(DashboardService);
  private readonly reporteService = inject(ReporteService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly apiErrorStore = inject(ApiErrorStore);
  private socioRutaId: number | null = null;
  private actividadRutaId: number | null = null;

  protected loggedIn = computed(() => this.sessionStore.usuario() !== null);
  protected apiError = this.apiErrorStore.message;
  protected activeSection = signal('dashboard');
  protected sidebarOpen = signal(true);
  protected userMenuOpen = signal(false);
  protected loginForm = this.formBuilder.nonNullable.group({
    dni: ['', [Validators.required, Validators.pattern(/^\d{7,11}$/)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  protected loginLoading = signal(false);
  protected loginError = signal('');
  protected passwordVisible = signal(false);
  protected passwordActual = '';
  protected nuevaPassword = '';
  protected confirmarNuevaPassword = '';
  protected passwordError = signal('');
  protected configPasswordActual = '';
  protected configNuevaPassword = '';
  protected configConfirmarNuevaPassword = '';
  protected configMensaje = signal('');
  protected configError = signal('');
  protected perfilNombre = '';
  protected perfilApellido = '';
  protected perfilMensaje = signal('');
  protected perfilError = signal('');
  protected currentUser = this.sessionStore.usuario;
  protected usuarios = signal<UsuarioApp[]>([]);
  protected usuarioAsignacionesEditando = signal<UsuarioApp | null>(null);
  protected actividadesPorClub = signal<Record<number, ActividadVista[]>>({});
  protected clubesDisponibles = signal<ClubAsignado[]>([]);
  protected clubActivoId = signal<number | null>(null);
  protected clubes = signal<ClubVista[]>([]);
  protected socios = signal<SocioVista[]>([]);
  protected actividades = signal<ActividadVista[]>([]);
  protected socioActividades = signal<Record<number, ActividadVista[]>>({});
  protected cuotas = signal<CuotaVista[]>([]);
  protected pagosPorCuota = signal<Partial<Record<number, PagoVista[]>>>({});
  protected cuotaPagosAbiertaId = signal<number | null>(null);
  protected cuotasCargadas = signal(false);
  protected asistencia = signal<AsistenciaVista[]>([]);
  protected dashboard = signal<DashboardData | null>(null);
  protected reportes = signal<ReportesData | null>(null);
  protected usuarioError = signal('');
  protected clubError = signal('');
  protected clubMensaje = signal('');
  protected socioBusqueda = signal('');
  protected socioApellidoFiltro = signal('');
  protected socioEstadoFiltro = signal('TODOS');
  protected socioActividadFiltro = signal('TODAS');
  protected socioEdadFiltro = signal('TODOS');
  protected socioSeleccionado = signal<SocioVista | null>(null);
  protected socioEditando = signal<SocioVista | null>(null);
  protected socioForm: SocioVista = this.crearSocioVacio();
  protected nuevoSocioForm: SocioVista = this.crearSocioVacio();
  protected clubPanelAbierto = signal(false);
  protected clubEditando = signal<ClubVista | null>(null);
  protected clubForm: ClubVista = this.crearClubVacio();
  protected socioActividadesSeleccionadas = signal<number[]>([]);
  protected selectedActivityId = signal<number | null>(null);
  protected selectedAttendanceDate = signal(this.fechaActualIso());
  protected asistenciaBusqueda = signal('');
  protected asistenciaEstadoFiltro = signal('TODOS');
  protected selectedMember = signal('TODOS');
  protected cuotaPeriodo = this.periodoActualIso();
  protected cuotaPeriodoFiltro = 'TODOS';
  protected cuotaEstadoFiltro = 'TODOS';
  protected cuotaImporte = 6500;
  protected cuotaVencimiento = `${this.periodoActualIso()}-10`;
  protected reporteAnio = new Date().getFullYear();
  protected reporteDesde = '';
  protected reporteHasta = '';
  protected reporteActividadId = 'TODAS';
  protected reporteEstadoSocio = 'TODOS';
  protected medioPago: MedioPago = 'EFECTIVO';
  protected motivoAnulacionPago = '';
  protected cuotaMensaje = signal('');
  protected dataError = signal('');
  protected nuevaActividad = {
    nombre: '',
    profesor: '',
    profesorUsuarioId: null as number | null,
    dias: '',
    categoria: 'Bienestar',
    icono: 'check',
    cupo: 20,
  };
  protected actividadEditando = signal<ActividadVista | null>(null);
  protected actividadForm = {
    nombre: '',
    profesor: '',
    profesorUsuarioId: null as number | null,
    dias: '',
    categoria: 'Bienestar',
    icono: 'check',
    cupo: 20,
    estado: 'ACTIVA' as EstadoActividad,
  };
  protected nuevoUsuario = {
    dni: '',
    nombre: '',
    apellido: '',
    rol: 'PROFESOR' as RolUsuario,
    clubId: null as number | null,
    rolClub: 'PROFESOR' as RolClub,
    passwordInicial: '',
  };
  protected asignacionesUsuarioForm: AsignacionUsuarioForm[] = [];

  protected readonly baseNavItems = [
    { id: 'dashboard', label: 'Dashboard', icon: 'home' },
    { id: 'socios', label: 'Socios', icon: 'users' },
    { id: 'clubes', label: 'Clubes', icon: 'home' },
    { id: 'actividades', label: 'Actividades', icon: 'music' },
    { id: 'inscripciones', label: 'Inscripciones', icon: 'add' },
    { id: 'asistencias', label: 'Asistencias', icon: 'check' },
    { id: 'cuotas', label: 'Cuotas y pagos', icon: 'receipt' },
    { id: 'reportes', label: 'Reportes', icon: 'chart' },
    { id: 'usuarios', label: 'Usuarios y roles', icon: 'key' },
    { id: 'configuracion', label: 'Configuracion', icon: 'settings' },
  ];
  protected readonly diasSemana = ['Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado', 'Domingo'];
  protected nuevaActividadDia = 'Martes';
  protected nuevaActividadHora = '10:00';
  protected actividadFormDia = 'Martes';
  protected actividadFormHora = '10:00';

  constructor() {
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(),
    ).subscribe((event) => this.activarSeccionDesdeUrl(event.urlAfterRedirects));
    this.restaurarSesion();
  }

  @HostListener('document:keydown.escape')
  protected cerrarSuperficiesConEscape(): void {
    this.userMenuOpen.set(false);
    if (this.socioEditando()) {
      this.cerrarPanelSocio();
    }
    if (this.clubPanelAbierto()) {
      this.cerrarPanelClub();
    }
  }

  protected navItems = computed(() => this.baseNavItems.filter((item) => {
    if (item.id === 'usuarios') {
      return this.puedeAdministrarUsuarios();
    }
    if (this.currentUser()?.rol === 'OPERADOR') {
      return item.id !== 'clubes';
    }
    if (this.currentUser()?.rol === 'PROFESOR') {
      return ['actividades', 'asistencias', 'configuracion'].includes(item.id);
    }

    return true;
  }));

  protected sectionTitle = computed(() => this.navItems().find((item) => item.id === this.activeSection())?.label ?? 'Inicio');

  protected sociosFiltrados = computed(() => {
    const busqueda = this.socioBusqueda().trim().toLowerCase();
    const apellido = this.socioApellidoFiltro().trim().toLowerCase();
    const estado = this.socioEstadoFiltro();
    const actividad = this.socioActividadFiltro();
    const edad = this.socioEdadFiltro();

    return this.socios().filter((socio) => {
      const coincideEstado = estado === 'TODOS' || socio.estado === estado;
      const coincideApellido = !apellido || socio.apellido.toLowerCase().includes(apellido);
      const coincideActividad = actividad === 'TODAS' || this.socioActividadesPreview(socio).some((item) => item.nombre === actividad);
      const coincideEdad = edad === 'TODOS' || this.coincideRangoEdad(this.socioEdad(socio), edad);
      const texto = `${socio.nombre} ${socio.apellido} ${socio.dni} ${socio.telefono}`.toLowerCase();
      return coincideEstado && coincideApellido && coincideActividad && coincideEdad && (!busqueda || texto.includes(busqueda));
    });
  });

  protected cuotasFiltradas = computed(() => {
    const socioId = this.selectedMember();
    const busqueda = this.socioBusqueda().trim().toLowerCase();
    return this.cuotas().filter((cuota) => {
      const coincideSocio = socioId === 'TODOS' || String(cuota.socioId) === socioId;
      const coincidePeriodo = this.cuotaPeriodoFiltro === 'TODOS' || cuota.periodo === this.cuotaPeriodoFiltro || cuota.mes === this.cuotaPeriodoFiltro;
      const coincideEstado = this.cuotaEstadoFiltro === 'TODOS' || cuota.estado === this.cuotaEstadoFiltro;
      const texto = `${cuota.socioNombre} ${cuota.socioDni} ${cuota.mes} ${cuota.estado}`.toLowerCase();
      return coincideSocio && coincidePeriodo && coincideEstado && (!busqueda || texto.includes(busqueda));
    });
  });

  protected periodosCuotas = computed(() => Array.from(new Set(this.cuotas().map((cuota) => cuota.periodo ?? cuota.mes))).sort().reverse());

  protected cuotasResumenSocios = computed(() => {
    const busqueda = this.socioBusqueda().trim().toLowerCase();

    return this.socios()
      .filter((socio) => {
        const texto = `${socio.nombre} ${socio.apellido} ${socio.dni} ${socio.telefono}`.toLowerCase();
        return !busqueda || texto.includes(busqueda);
      })
      .map((socio) => {
        const cuotasSocio = this.cuotas().filter((cuota) => cuota.socioId === socio.id);
        const pendientes = cuotasSocio.filter((cuota) => cuota.estado !== 'PAGADA').length;
        const ultimaCuota = [...cuotasSocio].sort((a, b) => b.vencimiento.localeCompare(a.vencimiento))[0];

        return {
          socioId: socio.id,
          socioNombre: `${socio.nombre} ${socio.apellido}`,
          socioDni: socio.dni,
          estado: pendientes === 0 ? 'AL_DIA' : 'NO_AL_DIA',
          pendientes,
          ultimoMes: ultimaCuota?.mes ?? 'Sin cuotas',
        } satisfies CuotaResumenSocio;
      });
  });

  protected sociosMorosos = computed(() => {
    if (!this.cuotasCargadas()) {
      return this.dashboard()?.morosos ?? 0;
    }

    return this.cuotasResumenSocios().filter((resumen) => resumen.estado === 'NO_AL_DIA').length;
  });

  protected sociosAlDia = computed(() => Math.max(0, this.socios().length - this.sociosMorosos()));

  protected metrics = computed(() => {
    const dashboard = this.dashboard();
    return [
      { label: 'Clubes activos', value: String(this.clubes().filter((club) => club.estado === 'ACTIVO').length), change: `${this.clubes().length} disponibles para tu rol`, tone: 'info' },
      { label: 'Socios activos', value: String(this.socios().filter((socio) => socio.estado === 'ACTIVO').length || dashboard?.sociosActivos || 0), change: `${this.socios().length || dashboard?.sociosTotales || 0} socios totales`, tone: 'success' },
      { label: 'Actividades', value: String(this.actividades().filter((actividad) => actividad.estado === 'ACTIVA').length || dashboard?.actividadesActivas || 0), change: `${this.actividades().length} actividades registradas`, tone: 'violet' },
      { label: 'Inscripciones', value: String(this.inscriptosTotalActividades()), change: 'Asignaciones activas a talleres', tone: 'info' },
      { label: 'Asistencias', value: String(dashboard?.asistenciaMes ?? 0), change: 'Registros del periodo actual', tone: 'warning' },
      { label: 'Recaudacion', value: this.formatearImporte(dashboard?.totalCobrado ?? 0), change: 'Total cobrado en el periodo', tone: 'success' },
    ];
  });

  protected actividadesProximasDelClub = computed(() => this.dashboard()?.proximasActividades ?? []);
  protected cuotasPendientesDelClub = computed(() => this.cuotasCargadas() ? this.cuotas().filter((cuota) => cuota.estado !== 'PAGADA').slice(0, 4) : this.dashboard()?.cuotasPendientes ?? []);
  protected sociosRecientesDelClub = computed(() => this.dashboard()?.sociosRecientes ?? []);
  protected sociosActivosClub = computed(() => this.socios().filter((socio) => socio.estado === 'ACTIVO').length);
  protected sociosInactivosClub = computed(() => this.socios().filter((socio) => socio.estado === 'INACTIVO').length);
  protected totalDeudaClub = computed(() => this.cuotas().filter((cuota) => cuota.estado !== 'PAGADA' && cuota.estado !== 'ANULADA').reduce((total, cuota) => total + cuota.importe, 0));
  protected cantidadCuotasDeuda = computed(() => this.cuotas().filter((cuota) => cuota.estado !== 'PAGADA' && cuota.estado !== 'ANULADA').length);
  protected actividadesActivasClub = computed(() => this.actividades().filter((actividad) => actividad.estado === 'ACTIVA').length);
  protected cupoTotalActividades = computed(() => this.actividades().reduce((total, actividad) => total + actividad.cupo, 0));
  protected inscriptosTotalActividades = computed(() => this.actividades().reduce((total, actividad) => total + actividad.inscriptos, 0));
  protected profesoresDelClubActual = computed(() => {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return [];
    }

    return this.usuarios()
      .filter((usuario) => usuario.estado === 'ACTIVO')
      .filter((usuario) => usuario.rol === 'PROFESOR')
      .filter((usuario) => usuario.clubes.some((club) => club.clubId === clubId && club.rol === 'PROFESOR'))
      .sort((a, b) => `${a.apellido} ${a.nombre}`.localeCompare(`${b.apellido} ${b.nombre}`));
  });
  protected deudaPorEstado = computed(() => ({
    pendiente: this.cuotas().filter((cuota) => cuota.estado === 'PENDIENTE').reduce((total, cuota) => total + cuota.importe, 0),
    vencida: this.cuotas().filter((cuota) => cuota.estado === 'VENCIDA').reduce((total, cuota) => total + cuota.importe, 0),
  }));
  protected totalDeudaPeriodo = computed(() => this.deudaPorEstado().pendiente + this.deudaPorEstado().vencida);
  protected porcentajeCobranza = computed(() => {
    const cobrado = this.dashboard()?.totalCobrado ?? 0;
    const total = cobrado + this.totalDeudaPeriodo();
    return total > 0 ? Math.round((cobrado / total) * 100) : 0;
  });
  protected distribucionSociosActivos = computed(() => {
    const total = this.socios().length;
    return total > 0 ? Math.round((this.sociosActivosClub() / total) * 100) : 0;
  });
  protected alertasGestion = computed(() => {
    const alertas: { titulo: string; detalle: string; tono: 'danger' | 'warning' | 'info' }[] = [];
    const vencidas = this.cuotas().filter((cuota) => cuota.estado === 'VENCIDA');
    const completas = this.actividades().filter((actividad) => actividad.cupo > 0 && actividad.inscriptos >= actividad.cupo);
    if (vencidas.length > 0) {
      alertas.push({ titulo: `${vencidas.length} cuotas vencidas`, detalle: `${this.formatearImporte(this.deudaPorEstado().vencida)} requieren seguimiento`, tono: 'danger' });
    }
    if (completas.length > 0) {
      alertas.push({ titulo: `${completas.length} actividades sin cupo`, detalle: 'Revisa la capacidad antes de nuevas inscripciones', tono: 'warning' });
    }
    if (this.socios().some((socio) => !socio.telefono || !socio.direccion)) {
      alertas.push({ titulo: 'Informacion incompleta', detalle: 'Hay socios con datos de contacto pendientes', tono: 'info' });
    }
    return alertas;
  });
  protected asistenciaBars = computed(() => this.normalizarBarras(this.dashboard()?.asistenciaMensual ?? this.reportes()?.asistenciaMensual ?? []));
  protected cumpleaniosDelMes = computed(() => {
    const mesActual = new Date().getMonth();
    return this.socios()
      .map((socio) => {
        const nacimiento = this.fechaNacimientoSocio(socio);
        const edad = this.socioEdad(socio);
        return nacimiento && edad !== null ? { socio, nacimiento, edad } : null;
      })
      .filter((cumple): cumple is { socio: SocioVista; nacimiento: Date; edad: number } => cumple !== null && cumple.nacimiento.getMonth() === mesActual)
      .sort((a, b) => a.nacimiento.getDate() - b.nacimiento.getDate())
      .slice(0, 4);
  });
  protected socioPreview = computed(() => this.socioSeleccionado() ?? this.sociosFiltrados()[0] ?? null);
  protected actividadesFiltro = computed(() => {
    const nombres = new Set<string>();
    Object.values(this.socioActividades()).forEach((actividades) => actividades.forEach((actividad) => nombres.add(actividad.nombre)));
    return Array.from(nombres).length > 0 ? Array.from(nombres) : this.actividades().map((actividad) => actividad.nombre);
  });
  protected actividadesParaAsistencia = computed(() => this.actividades().filter((actividad) => this.esFechaDeClase(actividad.dias, this.selectedAttendanceDate())));
  protected actividadSeleccionada = computed(() => this.actividades().find((actividad) => actividad.id === this.selectedActivityId()) ?? null);
  protected actividadDetalleRuta = computed(() => this.actividadRutaId
    ? this.actividades().find((actividad) => actividad.id === this.actividadRutaId) ?? null
    : null);
  protected asistenciaFiltrada = computed(() => {
    const busqueda = this.asistenciaBusqueda().trim().toLowerCase();
    const estado = this.asistenciaEstadoFiltro();

    return this.asistencia().filter((item) => {
      const coincideEstado = estado === 'TODOS' || item.estado === estado;
      const texto = `${item.socioNombre} ${item.socioDni} ${item.socioTelefono}`.toLowerCase();
      return coincideEstado && (!busqueda || texto.includes(busqueda));
    });
  });
  protected asistenciaPresentes = computed(() => this.asistencia().filter((item) => item.estado === 'PRESENTE').length);
  protected asistenciaAusentes = computed(() => this.asistencia().filter((item) => item.estado === 'AUSENTE').length);
  protected asistenciaJustificadas = computed(() => this.asistencia().filter((item) => item.estado === 'JUSTIFICADO').length);
  protected asistenciaPorcentaje = computed(() => {
    const total = this.asistencia().length;
    return total === 0 ? 0 : Math.round((this.asistenciaPresentes() / total) * 100);
  });
  protected esFechaDeClaseSeleccionada = computed(() => {
    const actividad = this.actividadSeleccionada();
    return actividad ? this.esFechaDeClase(actividad.dias, this.selectedAttendanceDate()) : false;
  });
  protected puedeEditarAsistencia = computed(() => {
    if (!this.esFechaDeClaseSeleccionada()) {
      return false;
    }

    return this.puedeOperarClubActual() || this.selectedAttendanceDate() === this.fechaActualIso();
  });
  protected avisoAsistencia = computed(() => {
    if (!this.actividadSeleccionada()) {
      return 'Selecciona una actividad para tomar asistencia.';
    }
    if (!this.esFechaDeClaseSeleccionada()) {
      return 'La fecha elegida no corresponde a los dias de clase de esta actividad.';
    }
    if (!this.puedeOperarClubActual() && this.selectedAttendanceDate() !== this.fechaActualIso()) {
      return 'Los profesores solo pueden guardar asistencia el dia de la clase.';
    }
    return '';
  });

  protected nombreClubActivo(): string {
    const clubActivoId = this.clubActivoId();
    return this.clubesDisponibles().find((club) => club.clubId === clubActivoId)?.clubNombre ?? 'Sin club';
  }

  protected logoClubActivo(): string | null {
    const clubActivoId = this.clubActivoId();
    return this.clubesDisponibles().find((club) => club.clubId === clubActivoId)?.logoUrl ?? null;
  }

  protected login(): void {
    this.loginError.set('');
    if (this.loginForm.invalid || this.loginLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }
    const credentials = this.loginForm.getRawValue();
    this.loginLoading.set(true);

    this.authService.login({
      dni: credentials.dni,
      password: credentials.password,
    }).pipe(finalize(() => this.loginLoading.set(false))).subscribe({
      next: (response) => {
        this.cargarFormularioPerfil(response.usuario);
        void this.router.navigate(['/dashboard']);
        this.passwordActual = credentials.password;
        this.loginForm.reset();
        this.cargarClubes();

        if (!response.usuario.debeCambiarPassword && this.puedeAdministrarUsuarios()) {
          this.cargarUsuarios();
        }
      },
      error: (error) => {
        this.loginError.set(error.status === 0 ? 'El backend no esta levantado. Inicia el backend y volve a intentar.' : 'DNI o contrasena incorrectos.');
      },
    });
  }

  protected cambiarClub(clubId: number): void {
    this.clubActivoId.set(Number(clubId));
    this.nuevoUsuario.clubId = Number(clubId);
    this.nuevoSocioForm = this.crearSocioVacio();
    this.selectedMember.set('TODOS');
    this.cerrarPanelSocio();
    this.cerrarPanelClub();
    this.cargarDatosClub();
  }

  protected goTo(section: string): void {
    void this.router.navigateByUrl(section === 'nuevo-socio' ? '/socios/nuevo' : `/${section}`);
  }

  private activarSeccionDesdeUrl(url: string): void {
    const path = url.split('?')[0].replace(/^\//, '');
    const socioMatch = path.match(/^socios\/(\d+)$/);
    const actividadMatch = path.match(/^actividades\/(\d+)$/);
    this.socioRutaId = socioMatch ? Number(socioMatch[1]) : null;
    this.actividadRutaId = actividadMatch ? Number(actividadMatch[1]) : null;
    const section = path === 'socios/nuevo'
      ? 'nuevo-socio'
      : path.startsWith('socios/')
        ? 'socios'
        : path.startsWith('actividades/')
          ? 'actividades'
          : path;
    if (!section || section === 'login' || !this.baseNavItems.some((item) => item.id === section)
      && section !== 'nuevo-socio' && section !== 'no-encontrado') {
      return;
    }
    this.activeSection.set(section);
    this.userMenuOpen.set(false);
    const socioDeRuta = this.socios().find((socio) => socio.id === this.socioRutaId);
    if (socioDeRuta) {
      this.prepararInscripcionesSocio(socioDeRuta);
    }
    if (this.actividadRutaId && this.actividades().some((actividad) => actividad.id === this.actividadRutaId)) {
      this.selectedActivityId.set(this.actividadRutaId);
    }

    if (section === 'usuarios' && this.puedeAdministrarUsuarios()) {
      this.cargarUsuarios();
    }
    if (section === 'actividades' && this.puedeAdministrarUsuarios()) {
      this.cargarUsuarios();
    }
    if (section === 'asistencias') {
      this.cargarAsistencia();
    }
    if (section === 'reportes') {
      this.cargarReportes();
    }
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected cerrarErrorGlobal(): void {
    this.apiErrorStore.clear();
  }

  protected toggleUserMenu(): void {
    this.userMenuOpen.update((open) => !open);
  }

  protected logout(): void {
    const limpiar = () => this.limpiarEstadoSesion();
    this.authService.logout().subscribe({
      error: limpiar,
      complete: limpiar,
    });
  }

  private limpiarEstadoSesion(): void {
    this.userMenuOpen.set(false);
    void this.router.navigate(['/login']);
    this.usuarios.set([]);
    this.clubesDisponibles.set([]);
    this.clubActivoId.set(null);
    this.clubes.set([]);
    this.socios.set([]);
    this.actividades.set([]);
    this.cuotas.set([]);
    this.cuotasCargadas.set(false);
    this.asistencia.set([]);
    this.dashboard.set(null);
    this.reportes.set(null);
    this.loginForm.reset();
  }

  protected debeCambiarPassword(): boolean {
    return this.currentUser()?.debeCambiarPassword === true;
  }

  protected cambiarPassword(): void {
    this.passwordError.set('');
    if (this.nuevaPassword !== this.confirmarNuevaPassword) {
      this.passwordError.set('Las contrasenas nuevas no coinciden.');
      return;
    }
    this.authService.changePassword(this.passwordActual, this.nuevaPassword).subscribe({
      next: (usuario) => {
        this.sessionStore.setUser(usuario);
        this.passwordActual = '';
        this.nuevaPassword = '';
        this.confirmarNuevaPassword = '';
        this.cargarClubes();
        if (this.puedeAdministrarUsuarios()) {
          this.cargarUsuarios();
        }
      },
      error: (error) => this.passwordError.set(error.status === 0 ? 'El backend no esta disponible.' : 'No se pudo cambiar la contrasena. Usa al menos 8 caracteres y revisa la actual.'),
    });
  }

  protected cambiarPasswordDesdeConfiguracion(): void {
    this.configMensaje.set('');
    this.configError.set('');
    if (this.configNuevaPassword !== this.configConfirmarNuevaPassword) {
      this.configError.set('Las contrasenas nuevas no coinciden.');
      return;
    }
    this.authService.changePassword(this.configPasswordActual, this.configNuevaPassword).subscribe({
      next: (usuario) => {
        this.sessionStore.setUser(usuario);
        this.configPasswordActual = '';
        this.configNuevaPassword = '';
        this.configConfirmarNuevaPassword = '';
        this.configMensaje.set('Contrasena actualizada correctamente.');
      },
      error: (error) => this.configError.set(error.status === 0 ? 'El backend no esta disponible.' : 'No se pudo cambiar la contrasena. Revisa la actual y usa al menos 8 caracteres.'),
    });
  }

  protected guardarPerfil(): void {
    this.perfilMensaje.set('');
    this.perfilError.set('');
    this.authService.updateProfile(this.perfilNombre, this.perfilApellido).subscribe({
      next: (usuario) => {
        this.sessionStore.setUser(usuario);
        this.cargarFormularioPerfil(usuario);
        this.perfilMensaje.set('Datos actualizados correctamente.');
      },
      error: (error) => this.perfilError.set(error.status === 0 ? 'El backend no esta disponible.' : 'No se pudieron actualizar los datos.'),
    });
  }

  protected puedeAdministrarUsuarios(): boolean {
    const usuario = this.currentUser();
    return usuario?.rol === 'SUPERUSUARIO' || usuario?.rol === 'ADMINISTRADOR';
  }

  protected esSuperusuario(): boolean {
    return this.currentUser()?.rol === 'SUPERUSUARIO';
  }

  protected esAdministradorClubActual(): boolean {
    const usuario = this.currentUser();
    const clubId = this.clubActivoId();
    return usuario?.rol === 'SUPERUSUARIO' || usuario?.clubes.some((club) => club.clubId === clubId && club.rol === 'ADMINISTRADOR') === true;
  }

  protected puedeOperarClubActual(): boolean {
    const usuario = this.currentUser();
    const clubId = this.clubActivoId();
    return usuario?.rol === 'SUPERUSUARIO' || usuario?.clubes.some((club) =>
      club.clubId === clubId && (club.rol === 'ADMINISTRADOR' || club.rol === 'OPERADOR')) === true;
  }

  protected nombreUsuarioActual(): string {
    const usuario = this.currentUser();
    return usuario ? `${usuario.nombre} ${usuario.apellido}` : 'Usuario';
  }

  protected inicialesUsuarioActual(): string {
    const usuario = this.currentUser();
    return usuario ? `${usuario.nombre.charAt(0)}${usuario.apellido.charAt(0)}`.toUpperCase() : 'RC';
  }

  protected togglePasswordVisibility(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  protected rolVisible(rol: RolUsuario): string {
    return { SUPERUSUARIO: 'Superusuario', ADMINISTRADOR: 'Administrador', OPERADOR: 'Operador', PROFESOR: 'Profesor' }[rol];
  }

  protected sincronizarRolNuevoUsuario(rol: RolUsuario): void {
    this.nuevoUsuario.rol = rol;
    this.nuevoUsuario.rolClub = this.rolClubEsperado(rol);
  }

  private rolClubEsperado(rol: RolUsuario): RolClub {
    return rol === 'ADMINISTRADOR' ? 'ADMINISTRADOR' : rol === 'OPERADOR' ? 'OPERADOR' : 'PROFESOR';
  }

  protected cargarUsuarios(): void {
    this.usuarioError.set('');
    this.usuarioService.listar().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        usuarios.flatMap((usuario) => usuario.clubes).forEach((club) => this.cargarActividadesParaAsignacion(club.clubId));
      },
      error: () => this.usuarioError.set('No se pudieron cargar los usuarios. Revisa que el backend este levantado.'),
    });
  }

  protected crearUsuario(): void {
    this.usuarioError.set('');
    const payload = {
      ...this.nuevoUsuario,
      asignaciones: this.nuevoUsuario.rol === 'SUPERUSUARIO' ? [] : [{
        clubId: this.nuevoUsuario.clubId,
        rolClub: this.nuevoUsuario.rolClub,
        actividadIds: [],
      }],
    };
    this.usuarioService.crear(payload).subscribe({
      next: () => {
        this.nuevoUsuario = { dni: '', nombre: '', apellido: '', rol: 'PROFESOR', clubId: this.clubActivoId(), rolClub: 'PROFESOR', passwordInicial: '' };
        this.cargarUsuarios();
      },
      error: () => this.usuarioError.set('No se pudo crear el usuario. Revisa los datos y permisos.'),
    });
  }

  protected desactivarUsuario(id: number): void {
    this.usuarioError.set('');
    this.usuarioService.desactivar(id).subscribe({
      next: (usuarioActualizado) => this.actualizarUsuarioEnLista(usuarioActualizado),
      error: () => this.usuarioError.set('No se pudo desactivar el usuario.'),
    });
  }

  protected activarUsuario(id: number): void {
    this.usuarioError.set('');
    this.usuarioService.activar(id).subscribe({
      next: (usuarioActualizado) => this.actualizarUsuarioEnLista(usuarioActualizado),
      error: () => this.usuarioError.set('No se pudo activar el usuario.'),
    });
  }

  protected editarAsignacionesUsuario(usuario: UsuarioApp): void {
    this.usuarioError.set('');
    this.usuarioAsignacionesEditando.set(usuario);
    this.asignacionesUsuarioForm = usuario.clubes.length > 0
      ? usuario.clubes.map((club) => ({ clubId: club.clubId, rolClub: club.rol, actividadIds: [...(club.actividadIds ?? [])] }))
      : [{ clubId: this.clubActivoId(), rolClub: this.rolClubEsperado(usuario.rol), actividadIds: [] }];
    this.asignacionesUsuarioForm.forEach((asignacion) => {
      if (asignacion.clubId) {
        this.cargarActividadesParaAsignacion(asignacion.clubId);
      }
    });
  }

  protected cancelarAsignacionesUsuario(): void {
    this.usuarioAsignacionesEditando.set(null);
    this.asignacionesUsuarioForm = [];
  }

  protected agregarAsignacionUsuario(): void {
    const clubId = this.clubesDisponibles().find((club) => !this.asignacionesUsuarioForm.some((asignacion) => asignacion.clubId === club.clubId))?.clubId ?? null;
    const rol = this.usuarioAsignacionesEditando()?.rol ?? 'PROFESOR';
    this.asignacionesUsuarioForm.push({ clubId, rolClub: this.rolClubEsperado(rol), actividadIds: [] });
    if (clubId) {
      this.cargarActividadesParaAsignacion(clubId);
    }
  }

  protected quitarAsignacionUsuario(index: number): void {
    this.asignacionesUsuarioForm.splice(index, 1);
  }

  protected cambiarClubAsignacion(index: number, clubId: number | null): void {
    const asignacion = this.asignacionesUsuarioForm[index];
    if (!asignacion) {
      return;
    }
    asignacion.clubId = clubId === null ? null : Number(clubId);
    asignacion.actividadIds = [];
    if (asignacion.clubId) {
      this.cargarActividadesParaAsignacion(asignacion.clubId);
    }
  }

  protected toggleActividadAsignacion(index: number, actividadId: number, checked: boolean): void {
    const asignacion = this.asignacionesUsuarioForm[index];
    if (!asignacion) {
      return;
    }
    asignacion.actividadIds = checked
      ? Array.from(new Set([...asignacion.actividadIds, actividadId]))
      : asignacion.actividadIds.filter((id) => id !== actividadId);
  }

  protected actividadesDeAsignacion(clubId: number | null): ActividadVista[] {
    return clubId ? this.actividadesPorClub()[clubId] ?? [] : [];
  }

  protected actividadAsignada(asignacion: AsignacionUsuarioForm, actividadId: number): boolean {
    return asignacion.actividadIds.includes(actividadId);
  }

  protected guardarAsignacionesUsuario(): void {
    const usuario = this.usuarioAsignacionesEditando();
    if (!usuario) {
      return;
    }
    const asignaciones = this.asignacionesUsuarioForm
      .filter((asignacion) => asignacion.clubId)
      .map((asignacion) => ({
        clubId: asignacion.clubId,
        rolClub: asignacion.rolClub,
        actividadIds: asignacion.rolClub === 'PROFESOR' ? asignacion.actividadIds : [],
      }));

    this.usuarioError.set('');
    this.usuarioService.actualizarAsignaciones(usuario.id, asignaciones).subscribe({
      next: (usuarioActualizado) => {
        this.actualizarUsuarioEnLista(usuarioActualizado);
        this.cancelarAsignacionesUsuario();
      },
      error: () => this.usuarioError.set('No se pudieron guardar las asignaciones.'),
    });
  }

  protected editarClub(club: ClubVista): void {
    if (!this.esSuperusuario()) {
      return;
    }
    this.clubError.set('');
    this.clubMensaje.set('');
    this.clubEditando.set(club);
    this.clubForm = { ...club };
    this.clubPanelAbierto.set(true);
  }

  protected nuevoClub(): void {
    if (!this.esSuperusuario()) {
      return;
    }
    this.clubError.set('');
    this.clubMensaje.set('');
    this.clubEditando.set(null);
    this.clubForm = this.crearClubVacio();
    this.clubPanelAbierto.set(true);
  }

  protected guardarClub(): void {
    const editando = this.clubEditando();
    if (!this.esSuperusuario()) {
      return;
    }

    this.clubError.set('');
    this.clubMensaje.set('');

    const payload = {
      nombre: this.clubForm.nombre,
      direccion: this.clubForm.direccion,
      logoUrl: this.clubForm.logoUrl ?? null,
      estado: this.clubForm.estado,
    };
    const request = editando
      ? this.clubService.actualizar(editando.id, payload)
      : this.clubService.crear(payload);

    request.subscribe({
      next: (clubActualizado) => {
        if (editando) {
          this.clubes.update((clubes) => clubes.map((club) => club.id === clubActualizado.id ? clubActualizado : club));
        } else {
          this.clubes.update((clubes) => [...clubes, clubActualizado]);
        }
        this.sincronizarClubesDisponibles(this.clubes());
        this.clubActivoId.set(clubActualizado.id);
        this.nuevoUsuario.clubId = clubActualizado.id;
        this.cargarDatosClub();
        this.cerrarPanelClub();
        this.clubMensaje.set(editando ? 'Club actualizado correctamente.' : 'Club creado correctamente.');
      },
      error: () => this.clubError.set('No se pudo guardar el club. Revisa los datos y permisos.'),
    });
  }

  protected cargarLogoClub(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) {
      return;
    }
    if (!archivo.type.startsWith('image/')) {
      this.clubError.set('Selecciona una imagen valida para el logo.');
      input.value = '';
      return;
    }
    if (archivo.size > 1_000_000) {
      this.clubError.set('El logo no puede superar 1 MB.');
      input.value = '';
      return;
    }

    const lector = new FileReader();
    lector.onload = () => {
      this.clubForm.logoUrl = String(lector.result);
      this.clubError.set('');
    };
    lector.readAsDataURL(archivo);
  }

  protected quitarLogoClub(): void {
    this.clubForm.logoUrl = null;
  }

  protected verSocio(socio: SocioVista): void {
    this.socioSeleccionado.set(socio);
  }

  protected editarSocio(socio: SocioVista): void {
    this.dataError.set('');
    this.socioEditando.set(socio);
    this.socioForm = { ...socio };
  }

  protected prepararInscripcionesSocio(socio: SocioVista): void {
    this.verSocio(socio);
    this.socioActividadesSeleccionadas.set((this.socioActividades()[socio.id] ?? []).map((actividad) => actividad.id));
    this.cargarActividadesDeSocio(socio);
  }

  protected guardarSocioEditado(): void {
    const clubId = this.clubActivoId();
    const editando = this.socioEditando();
    if (!clubId || !editando) {
      return;
    }
    this.dataError.set('');

    if (!this.socioForm.nombre.trim() || !this.socioForm.apellido.trim() || !this.socioForm.dni.trim()) {
      this.dataError.set('Completa nombre, apellido y DNI para guardar los cambios.');
      return;
    }

    const payload = {
      nombre: this.socioForm.nombre.trim(),
      apellido: this.socioForm.apellido.trim(),
      dni: this.socioForm.dni.trim(),
      email: this.socioForm.email.trim(),
      fechaNacimiento: this.normalizarFechaFormulario(this.socioForm.fechaNacimiento),
      telefono: this.socioForm.telefono.trim(),
      direccion: this.socioForm.direccion.trim(),
      emergenciaNombre: this.socioForm.emergenciaNombre.trim(),
      emergenciaTelefono: this.socioForm.emergenciaTelefono.trim(),
      emergenciaRelacion: this.socioForm.emergenciaRelacion.trim(),
      estado: this.socioForm.estado,
    };

    this.socioService.actualizar(clubId, editando.id, payload).subscribe({
      next: (socioActualizado) => {
        this.cerrarPanelSocio();
        this.cargarDatosClub(socioActualizado.id);
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo guardar el socio.'),
    });
  }

  protected guardarNuevoSocio(): void {
    const clubId = this.clubActivoId();
    this.dataError.set('');

    if (!clubId || !this.nuevoSocioForm.nombre.trim() || !this.nuevoSocioForm.apellido.trim() || !this.nuevoSocioForm.dni.trim()) {
      this.dataError.set('Completa nombre, apellido y DNI para guardar el socio.');
      return;
    }

    const payload = {
      nombre: this.nuevoSocioForm.nombre.trim(),
      apellido: this.nuevoSocioForm.apellido.trim(),
      dni: this.nuevoSocioForm.dni.trim(),
      email: this.nuevoSocioForm.email.trim(),
      fechaNacimiento: this.normalizarFechaFormulario(this.nuevoSocioForm.fechaNacimiento),
      telefono: this.nuevoSocioForm.telefono.trim(),
      direccion: this.nuevoSocioForm.direccion.trim(),
      emergenciaNombre: this.nuevoSocioForm.emergenciaNombre.trim(),
      emergenciaTelefono: this.nuevoSocioForm.emergenciaTelefono.trim(),
      emergenciaRelacion: this.nuevoSocioForm.emergenciaRelacion.trim(),
      estado: this.nuevoSocioForm.estado,
    };

    this.socioService.crear(clubId, payload).subscribe({
      next: (socioCreado) => {
        this.nuevoSocioForm = this.crearSocioVacio();
        this.goTo('socios');
        this.cargarDatosClub(socioCreado.id);
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo crear el socio. Revisa los datos y permisos.'),
    });
  }

  protected alternarEstadoSocio(socio: SocioVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    const actualizado = { ...socio, estado: socio.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO' as EstadoSocio };
    this.socioService.desactivar(clubId, socio.id, actualizado).subscribe({
      next: () => this.cargarDatosClub(),
      error: () => this.dataError.set('No se pudo cambiar el estado del socio.'),
    });
  }

  protected registrarPago(cuota: CuotaVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.cuotaMensaje.set('');
    this.pagoService.registrar(clubId, cuota.id, this.medioPago).subscribe({
      next: () => {
        this.cuotaMensaje.set('Pago registrado correctamente.');
        this.cargarDatosClub();
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo registrar el pago.'),
    });
  }

  protected generarCuotasMensuales(): void {
    const clubId = this.clubActivoId();
    if (!clubId || !this.cuotaPeriodo || this.cuotaImporte <= 0 || !this.cuotaVencimiento) {
      this.dataError.set('Completa periodo, importe y vencimiento.');
      return;
    }
    this.cuotaMensaje.set('');
    this.cuotaService.generar(clubId, {
      periodo: this.cuotaPeriodo,
      importe: this.cuotaImporte,
      vencimiento: this.cuotaVencimiento,
    }).subscribe({
      next: (resultado) => {
        this.cuotaMensaje.set(`Generacion completa: ${resultado.creadas} creadas y ${resultado.omitidas} omitidas.`);
        this.cargarCuotas();
        this.cargarDashboard();
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudieron generar las cuotas.'),
    });
  }

  protected crearCuotaIndividual(): void {
    const clubId = this.clubActivoId();
    const socioId = Number(this.selectedMember());
    if (!clubId || this.selectedMember() === 'TODOS' || !Number.isFinite(socioId)) {
      this.dataError.set('Selecciona un socio para crear una cuota individual.');
      return;
    }
    this.cuotaService.crear(clubId, {
      socioId,
      periodo: this.cuotaPeriodo,
      importe: this.cuotaImporte,
      vencimiento: this.cuotaVencimiento,
    }).subscribe({
      next: () => {
        this.cuotaMensaje.set('Cuota individual creada correctamente.');
        this.cargarCuotas();
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo crear la cuota.'),
    });
  }

  protected verHistorialPagos(cuota: CuotaVista): void {
    if (this.cuotaPagosAbiertaId() === cuota.id) {
      this.cuotaPagosAbiertaId.set(null);
      return;
    }
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.pagoService.listar(clubId, cuota.id).subscribe({
      next: (pagos) => {
        this.pagosPorCuota.update((actual) => ({ ...actual, [cuota.id]: pagos }));
        this.cuotaPagosAbiertaId.set(cuota.id);
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo cargar el historial de pagos.'),
    });
  }

  protected anularPago(cuota: CuotaVista, pago: PagoVista): void {
    const clubId = this.clubActivoId();
    const motivo = this.motivoAnulacionPago.trim();
    if (!clubId || !motivo) {
      this.dataError.set('Indica el motivo de anulacion.');
      return;
    }
    this.pagoService.anular(clubId, cuota.id, pago.id, motivo).subscribe({
      next: () => {
        this.motivoAnulacionPago = '';
        this.cuotaMensaje.set('Pago anulado; la cuota volvio a estado pendiente o vencido.');
        this.cargarCuotas();
        this.cuotaPagosAbiertaId.set(null);
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo anular el pago.'),
    });
  }

  protected socioActividadSeleccionada(actividadId: number): boolean {
    return this.socioActividadesSeleccionadas().includes(actividadId);
  }

  protected alternarActividadSocio(actividadId: number, seleccionada: boolean): void {
    this.socioActividadesSeleccionadas.update((actuales) => {
      if (seleccionada) {
        return actuales.includes(actividadId) ? actuales : [...actuales, actividadId];
      }
      return actuales.filter((id) => id !== actividadId);
    });
  }

  protected guardarInscripcionesSocio(socio: SocioVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }

    this.inscripcionService.actualizar(clubId, socio.id, this.socioActividadesSeleccionadas()).subscribe({
      next: (actividades) => {
        this.socioActividades.update((actual) => ({ ...actual, [socio.id]: actividades }));
        this.cargarActividades();
        if (this.activeSection() === 'asistencias') {
          this.cargarAsistencia();
        }
      },
      error: () => this.dataError.set('No se pudieron guardar las actividades del socio.'),
    });
  }

  protected verDetalleCuotasSocio(socioId: number): void {
    this.selectedMember.set(String(socioId));
  }

  protected crearActividad(): void {
    const clubId = this.clubActivoId();
    this.dataError.set('');
    if (!clubId || !this.nuevaActividad.nombre.trim() || !this.nuevaActividad.profesorUsuarioId || !this.nuevaActividadHora) {
      if (clubId && this.profesoresDelClubActual().length === 0) {
        this.dataError.set('Primero crea o asigna un profesor activo a este club.');
      }
      return;
    }

    const payload = {
      ...this.nuevaActividad,
      dias: this.formatearDiaHorario(this.nuevaActividadDia, this.nuevaActividadHora),
    };

    this.actividadService.crear(clubId, payload).subscribe({
      next: (actividad) => {
        this.nuevaActividad = {
          nombre: '',
          profesor: '',
          profesorUsuarioId: null,
          dias: '',
          categoria: 'Bienestar',
          icono: 'check',
          cupo: 20,
        };
        this.nuevaActividadDia = 'Martes';
        this.nuevaActividadHora = '10:00';
        this.selectedActivityId.set(actividad.id);
        this.cargarDatosClub();
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo crear la actividad.'),
    });
  }

  protected editarActividad(actividad: ActividadVista): void {
    if (!this.esAdministradorClubActual()) {
      return;
    }

    const horario = this.extraerDiaHorario(actividad.dias);
    this.actividadEditando.set(actividad);
    this.actividadForm = {
      nombre: actividad.nombre,
      profesor: actividad.profesor,
      profesorUsuarioId: actividad.profesorUsuarioId
        ?? this.profesoresDelClubActual().find((profesor) => `${profesor.nombre} ${profesor.apellido}` === actividad.profesor)?.id
        ?? null,
      dias: actividad.dias,
      categoria: actividad.categoria,
      icono: actividad.icono,
      cupo: actividad.cupo,
      estado: actividad.estado,
    };
    this.actividadFormDia = horario.dia;
    this.actividadFormHora = horario.hora;
  }

  protected guardarActividadEditada(): void {
    const clubId = this.clubActivoId();
    const actividad = this.actividadEditando();
    this.dataError.set('');

    if (!clubId || !actividad || !this.esAdministradorClubActual() || !this.actividadForm.profesorUsuarioId || !this.actividadFormHora) {
      if (clubId && this.profesoresDelClubActual().length === 0) {
        this.dataError.set('Primero crea o asigna un profesor activo a este club.');
      }
      return;
    }

    const payload = {
      ...this.actividadForm,
      dias: this.formatearDiaHorario(this.actividadFormDia, this.actividadFormHora),
    };

    this.actividadService
      .actualizar(clubId, actividad.id, payload)
      .subscribe({
        next: () => {
          this.actividadEditando.set(null);
          this.cargarDatosClub();
        },
        error: (error) => {
          this.dataError.set(error.error?.message ?? 'No se pudo guardar la actividad.');
        },
      });
  }

  protected cancelarEdicionActividad(): void {
    this.actividadEditando.set(null);
  }

  protected seleccionarActividadAsistencia(actividad: ActividadVista): void {
    const horario = this.extraerDiaHorario(actividad.dias);
    if (this.puedeOperarClubActual() && !this.esFechaDeClase(actividad.dias, this.selectedAttendanceDate())) {
      this.selectedAttendanceDate.set(this.proximaFechaParaDia(horario.dia));
    }
    this.selectedActivityId.set(actividad.id);
    this.goTo('asistencias');
    this.cargarAsistencia();
  }

  protected cambiarActividadAsistencia(actividadId: number | null): void {
    this.selectedActivityId.set(actividadId === null ? null : Number(actividadId));
    this.cargarAsistencia();
  }

  protected cambiarFechaAsistencia(fecha: string): void {
    this.selectedAttendanceDate.set(fecha);
    this.cargarAsistencia();
  }

  protected cargarAsistencia(): void {
    const clubId = this.clubActivoId();
    const actividadId = this.selectedActivityId();
    if (!clubId || !actividadId || !this.esFechaDeClaseSeleccionada()) {
      this.asistencia.set([]);
      return;
    }
    this.dataError.set('');
    this.asistenciaService.listar(clubId, actividadId, this.selectedAttendanceDate()).subscribe({
      next: (asistencia) => this.asistencia.set(asistencia),
      error: () => this.dataError.set('No se pudo cargar la asistencia.'),
    });
  }

  protected cambiarEstadoAsistencia(item: AsistenciaVista, estado: EstadoAsistencia): void {
    if (!this.puedeEditarAsistencia()) {
      return;
    }

    this.asistencia.update((asistencias) => asistencias.map((actual) => actual.socioId === item.socioId
      ? { ...actual, estado, presente: estado === 'PRESENTE' }
      : actual));
  }

  protected marcarTodosPresentes(): void {
    if (!this.puedeEditarAsistencia()) {
      return;
    }

    this.asistencia.update((asistencias) => asistencias.map((item) => ({ ...item, estado: 'PRESENTE', presente: true })));
  }

  protected guardarAsistencia(): void {
    const clubId = this.clubActivoId();
    const actividadId = this.selectedActivityId();
    if (!clubId || !actividadId || !this.puedeEditarAsistencia()) {
      return;
    }
    const payload = this.asistencia().map((item) => ({ socioId: item.socioId, estado: item.estado }));
    this.asistenciaService.guardar(clubId, actividadId, this.selectedAttendanceDate(), payload).subscribe({
      next: (asistencia) => {
        this.asistencia.set(asistencia);
        this.cargarDashboard();
      },
      error: (error) => this.dataError.set(error.error?.message ?? 'No se pudo guardar la asistencia.'),
    });
  }

  protected cerrarPanelSocio(): void {
    this.socioSeleccionado.set(null);
    this.socioEditando.set(null);
    this.socioForm = this.crearSocioVacio();
  }

  protected cerrarPanelClub(): void {
    this.clubPanelAbierto.set(false);
    this.clubEditando.set(null);
    this.clubForm = this.crearClubVacio();
  }

  protected formatearImporte(importe: number): string {
    return `$ ${new Intl.NumberFormat('es-AR').format(importe)}`;
  }

  protected socioEdad(socio: SocioVista): number | null {
    const nacimiento = this.fechaNacimientoSocio(socio);
    if (!nacimiento) {
      return null;
    }

    const hoy = new Date();
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const yaCumplio = hoy.getMonth() > nacimiento.getMonth() || (hoy.getMonth() === nacimiento.getMonth() && hoy.getDate() >= nacimiento.getDate());
    if (!yaCumplio) {
      edad--;
    }
    return edad;
  }

  protected socioNacimientoTexto(socio: SocioVista): string {
    const nacimiento = this.fechaNacimientoSocio(socio);
    if (nacimiento) {
      const edad = this.socioEdad(socio);
      return `${nacimiento.getDate()} de ${this.nombreMes(nacimiento)} de ${nacimiento.getFullYear()}${edad === null ? '' : ` (${edad} anios)`}`;
    }

    return 'Sin fecha cargada';
  }

  protected socioNumero(socio: SocioVista): string {
    return String(socio.numeroSocio ?? socio.id);
  }

  protected socioAltaTexto(socio: SocioVista): string {
    if (!socio.fechaAlta) {
      return 'Sin fecha de alta';
    }

    const fecha = new Date(`${socio.fechaAlta}T12:00:00`);
    if (Number.isNaN(fecha.getTime())) {
      return 'Sin fecha de alta';
    }

    return `${this.nombreMes(fecha)} de ${fecha.getFullYear()}`;
  }

  protected socioEdadTexto(socio: SocioVista): string {
    const edad = this.socioEdad(socio);
    return edad === null ? '-' : String(edad);
  }

  protected socioAvatarIniciales(socio: SocioVista): string {
    return `${socio.nombre.charAt(0)}${socio.apellido.charAt(0)}`.toUpperCase();
  }

  protected socioCuotaResumen(socio: SocioVista): { estado: string; importe: number } {
    if (!this.cuotasCargadas()) {
      return { estado: 'Cargando', importe: 0 };
    }

    const cuotasSocio = this.cuotas().filter((cuota) => cuota.socioId === socio.id);
    const pendiente = cuotasSocio.find((cuota) => cuota.estado !== 'PAGADA');
    return {
      estado: pendiente ? 'Moroso' : 'Al dia',
      importe: pendiente?.importe ?? 0,
    };
  }

  protected socioActividadesCount(socio: SocioVista): number {
    return this.socioActividadesPreview(socio).length;
  }

  protected socioActividadesPreview(socio: SocioVista): { nombre: string; dias: string; icono: string }[] {
    return (this.socioActividades()[socio.id] ?? []).map((actividad) => ({
      nombre: actividad.nombre,
      dias: actividad.dias,
      icono: actividad.icono,
    }));
  }

  protected estadoCuotaVisible(cuota: CuotaVista): string {
    return cuota.estado === 'PAGADA' ? 'Al dia' : 'No al dia';
  }

  protected talleresAsignadosTexto(club: ClubAsignado): string {
    if (club.rol === 'ADMINISTRADOR') {
      return 'Todos los talleres';
    }
    if (club.rol === 'OPERADOR') {
      return 'Todas las actividades (operacion)';
    }
    const nombres = (club.actividadIds ?? [])
      .map((actividadId) => this.actividadesPorClub()[club.clubId]?.find((actividad) => actividad.id === actividadId)?.nombre)
      .filter((nombre): nombre is string => Boolean(nombre));
    return nombres.length > 0 ? nombres.join(', ') : 'Sin talleres asignados';
  }

  protected alturaPagos(valor: number): number {
    const pagos = this.reportes()?.pagosPorMes.map((item) => item.valor) ?? [];
    const max = Math.max(1, ...pagos);
    return valor === 0 ? 4 : Math.max(8, Math.round((valor / max) * 100));
  }

  protected totalCobradoReporte = computed(() =>
    this.reportes()?.pagosPorMes.reduce((total, mes) => total + mes.valor, 0) ?? 0
  );
  protected sociosTotalReporte = computed(() => this.reportes()?.sociosPorClub[0]?.socios ?? this.socios().length);
  protected sociosActivosReporte = computed(() => this.reporteEstadoSocio === 'INACTIVO' ? 0
    : this.reporteEstadoSocio === 'ACTIVO' ? this.sociosTotalReporte() : this.sociosActivosClub());
  protected sociosInactivosReporte = computed(() => this.reporteEstadoSocio === 'ACTIVO' ? 0
    : this.reporteEstadoSocio === 'INACTIVO' ? this.sociosTotalReporte() : this.sociosInactivosClub());
  protected deudaReporte = computed(() => ({
    pendiente: this.reportes()?.deudaPendiente ?? 0,
    vencida: this.reportes()?.deudaVencida ?? 0,
    total: (this.reportes()?.deudaPendiente ?? 0) + (this.reportes()?.deudaVencida ?? 0),
    cantidad: this.reportes()?.cuotasConDeuda ?? 0,
  }));

  protected actualizarVencimientoCuota(periodo: string): void {
    if (/^\d{4}-\d{2}$/.test(periodo)) {
      this.cuotaVencimiento = `${periodo}-10`;
    }
  }

  protected cambiarAnioReporte(): void {
    const anio = Math.trunc(Number(this.reporteAnio));
    if (anio >= 2000 && anio <= 2100) {
      this.reporteAnio = anio;
      this.cargarReportes();
    }
  }

  protected aplicarFiltrosReporte(): void {
    if ((this.reporteDesde && !this.reporteHasta) || (!this.reporteDesde && this.reporteHasta)) {
      this.dataError.set('Completa ambas fechas del rango.');
      return;
    }
    this.cargarReportes();
  }

  private restaurarSesion(): void {
    this.authService.restore().subscribe((usuario) => {
      if (!usuario) {
        void this.router.navigate(['/login']);
        return;
      }

      this.cargarFormularioPerfil(usuario);
      const rutaActual = this.router.url.split('?')[0].replace(/^\//, '');
      const permitidaProfesor = ['actividades', 'asistencias', 'configuracion'].includes(rutaActual)
        || rutaActual.startsWith('actividades/');
      const permitidaOperador = !['usuarios', 'clubes'].includes(rutaActual);
      if (rutaActual === 'login' || (usuario.rol === 'PROFESOR' && !permitidaProfesor)
        || (usuario.rol === 'OPERADOR' && !permitidaOperador)) {
        void this.router.navigate([usuario.rol === 'PROFESOR' ? '/actividades' : '/dashboard']);
      }
      this.cargarClubes();
      if (!usuario.debeCambiarPassword && this.puedeAdministrarUsuarios()) {
        this.cargarUsuarios();
      }
    });
  }

  private cargarClubes(): void {
    this.clubService.listar().subscribe({
      next: (clubes) => {
        this.clubes.set(clubes);
        this.sincronizarClubesDisponibles(clubes);
        const clubInicial = this.clubesDisponibles()[0]?.clubId ?? null;
        this.clubActivoId.set(clubInicial);
        this.nuevoUsuario.clubId = clubInicial;
        if (clubInicial) {
          this.cargarDatosClub();
        } else {
          this.cargarDatosClub();
          if (this.esSuperusuario()) {
            this.goTo('clubes');
          }
        }
      },
      error: () => this.dataError.set('No se pudieron cargar los clubes.'),
    });
  }

  private sincronizarClubesDisponibles(clubes: ClubVista[]): void {
    this.clubesDisponibles.set(clubes.map((club) => ({
      clubId: club.id,
      clubNombre: club.nombre,
      logoUrl: club.logoUrl ?? null,
      rol: 'ADMINISTRADOR' as RolClub,
      actividadIds: [],
    })));
  }

  private cargarDatosClub(socioSeleccionadoId?: number): void {
    this.dataError.set('');
    this.socios.set([]);
    this.actividades.set([]);
    this.socioActividades.set({});
    this.cuotas.set([]);
    this.cuotasCargadas.set(false);
    this.asistencia.set([]);
    this.dashboard.set(null);
    this.socioSeleccionado.set(null);
    this.cargarActividades();
    if (this.currentUser()?.rol === 'PROFESOR') {
      return;
    }
    this.cargarSocios(socioSeleccionadoId);
    this.cargarCuotas();
    this.cargarDashboard();
    if (this.activeSection() === 'reportes') {
      this.cargarReportes();
    }
  }

  private cargarSocios(socioSeleccionadoId?: number): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.socioService.listar(clubId).subscribe({
      next: (socios) => {
        this.socios.set(socios);
        const socioObjetivoId = socioSeleccionadoId ?? this.socioRutaId;
        const socioParaSeleccionar = socios.find((socio) => socio.id === socioObjetivoId) ?? socios[0];
        if (!this.socioSeleccionado() && socioParaSeleccionar) {
          this.prepararInscripcionesSocio(socioParaSeleccionar);
        }
        this.cargarActividadesDeSocios(socios);
      },
      error: () => this.dataError.set('No se pudieron cargar los socios.'),
    });
  }

  private cargarActividadesDeSocios(socios: SocioVista[]): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }

    this.socioActividades.set({});
    socios.forEach((socio) => {
      this.cargarActividadesDeSocio(socio);
    });
  }

  private cargarActividadesDeSocio(socio: SocioVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }

    this.actividadService.listarDeSocio(clubId, socio.id).subscribe({
      next: (actividades) => {
        this.socioActividades.update((actual) => ({ ...actual, [socio.id]: actividades }));
        if (this.socioSeleccionado()?.id === socio.id) {
          this.socioActividadesSeleccionadas.set(actividades.map((actividad) => actividad.id));
        }
      },
    });
  }

  private cargarActividades(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.actividadService.listar(clubId).subscribe({
      next: (actividades) => {
        this.actividades.set(actividades);
        const actividadRuta = actividades.find((actividad) => actividad.id === this.actividadRutaId);
        if (actividadRuta || !this.selectedActivityId() && actividades.length > 0) {
          this.selectedActivityId.set(actividadRuta?.id ?? actividades[0].id);
        }
        if (this.activeSection() === 'asistencias') {
          this.cargarAsistencia();
        }
      },
      error: () => this.dataError.set('No se pudieron cargar las actividades.'),
    });
  }

  private cargarActividadesParaAsignacion(clubId: number): void {
    if (this.actividadesPorClub()[clubId]) {
      return;
    }
    this.actividadService.listar(clubId).subscribe({
      next: (actividades) => this.actividadesPorClub.update((actual) => ({ ...actual, [clubId]: actividades })),
      error: () => this.usuarioError.set('No se pudieron cargar los talleres del club.'),
    });
  }

  private cargarCuotas(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.cuotaService.listar(clubId).subscribe({
      next: (cuotas) => {
        this.cuotas.set(cuotas);
        this.cuotasCargadas.set(true);
      },
      error: () => {
        this.cuotasCargadas.set(true);
        this.dataError.set('No se pudieron cargar las cuotas.');
      },
    });
  }

  private cargarDashboard(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.dashboardService.cargar(clubId, this.periodoActualIso()).subscribe({
      next: (dashboard) => this.dashboard.set(dashboard),
      error: () => this.dataError.set('No se pudo cargar el dashboard.'),
    });
  }

  private cargarReportes(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.reporteService.cargar(clubId, this.reporteAnio, {
      desde: this.reporteDesde || undefined,
      hasta: this.reporteHasta || undefined,
      actividadId: this.reporteActividadId === 'TODAS' ? undefined : Number(this.reporteActividadId),
      estadoSocio: this.reporteEstadoSocio === 'TODOS' ? undefined : this.reporteEstadoSocio,
    }).subscribe({
      next: (reportes) => this.reportes.set(reportes),
      error: () => this.dataError.set('No se pudieron cargar los reportes.'),
    });
  }

  private cargarFormularioPerfil(usuario: UsuarioApp): void {
    this.perfilNombre = usuario.nombre;
    this.perfilApellido = usuario.apellido;
  }

  private actualizarUsuarioEnLista(usuarioActualizado: UsuarioApp): void {
    this.usuarios.update((usuarios) => usuarios.map((usuario) => usuario.id === usuarioActualizado.id ? usuarioActualizado : usuario));
  }

  private crearSocioVacio(): SocioVista {
    return {
      id: 0,
      clubId: this.clubActivoId() ?? 0,
      nombre: '',
      apellido: '',
      dni: '',
      email: '',
      fechaNacimiento: null,
      fechaAlta: null,
      numeroSocio: null,
      telefono: '',
      direccion: '',
      emergenciaNombre: '',
      emergenciaTelefono: '',
      emergenciaRelacion: '',
      estado: 'ACTIVO',
    };
  }

  private crearClubVacio(): ClubVista {
    return {
      id: 0,
      nombre: '',
      direccion: '',
      logoUrl: null,
      estado: 'ACTIVO',
    };
  }

  private normalizarBarras(datos: { valor: number }[]): number[] {
    const max = Math.max(1, ...datos.map((item) => item.valor));
    return datos.map((item) => Math.max(8, Math.round((item.valor / max) * 100)));
  }

  private coincideRangoEdad(edad: number | null, rango: string): boolean {
    if (edad === null) {
      return false;
    }

    if (rango === '60_69') {
      return edad >= 60 && edad <= 69;
    }
    if (rango === '70_79') {
      return edad >= 70 && edad <= 79;
    }
    if (rango === '80_MAS') {
      return edad >= 80;
    }
    return true;
  }

  protected fechaActualIso(): string {
    return this.formatearFechaIso(new Date());
  }

  private periodoActualIso(): string {
    const hoy = new Date();
    return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}`;
  }

  protected fechaCumpleTexto(fecha: Date): string {
    return `${fecha.getDate()} de ${this.nombreMes(fecha)}`;
  }

  private esFechaDeClase(dias: string, fechaIso: string): boolean {
    const fecha = new Date(`${fechaIso}T12:00:00`);
    const dia = fecha.getDay();
    const texto = dias.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const nombres = ['domingo', 'lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado'];
    return texto.includes(nombres[dia]);
  }

  private proximaFechaParaDia(dia: string): string {
    const nombres = ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];
    const objetivo = nombres.findIndex((nombre) => nombre.toLowerCase() === dia.toLowerCase());
    if (objetivo < 0) {
      return this.fechaActualIso();
    }

    const fecha = new Date();
    const diferencia = (objetivo - fecha.getDay() + 7) % 7;
    fecha.setDate(fecha.getDate() + diferencia);
    return this.formatearFechaIso(fecha);
  }

  private formatearDiaHorario(dia: string, hora: string): string {
    return `${dia} ${hora} hs`;
  }

  private extraerDiaHorario(texto: string): { dia: string; hora: string } {
    const normalizado = texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const dia = this.diasSemana.find((item) => normalizado.includes(item.toLowerCase())) ?? 'Martes';
    const hora = texto.match(/(\d{1,2}:\d{2})/)?.[1] ?? '10:00';
    return { dia, hora };
  }

  private formatearFechaIso(fecha: Date): string {
    const year = fecha.getFullYear();
    const month = String(fecha.getMonth() + 1).padStart(2, '0');
    const day = String(fecha.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private fechaNacimientoSocio(socio: SocioVista): Date | null {
    if (!socio.fechaNacimiento) {
      return null;
    }

    const fecha = new Date(`${socio.fechaNacimiento}T12:00:00`);
    return Number.isNaN(fecha.getTime()) ? null : fecha;
  }

  private nombreMes(fecha: Date): string {
    return new Intl.DateTimeFormat('es-AR', { month: 'long' }).format(fecha);
  }

  private normalizarFechaFormulario(fecha: string | null | undefined): string | null {
    if (!fecha) {
      return null;
    }

    const valor = fecha.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
      return valor;
    }

    const partes = valor.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (!partes) {
      return null;
    }

    const dia = partes[1].padStart(2, '0');
    const mes = partes[2].padStart(2, '0');
    return `${partes[3]}-${mes}-${dia}`;
  }
}
