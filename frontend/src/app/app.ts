import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

type RolUsuario = 'SUPERUSUARIO' | 'ADMINISTRADOR' | 'PROFESOR';
type EstadoUsuario = 'ACTIVO' | 'INACTIVO';
type RolClub = 'ADMINISTRADOR' | 'PROFESOR';
type EstadoSocio = 'ACTIVO' | 'INACTIVO';
type EstadoCuota = 'PENDIENTE' | 'PAGADO' | 'VENCIDA';

interface ClubAsignado {
  clubId: number;
  clubNombre: string;
  rol: RolClub;
}

interface ClubVista {
  id: number;
  nombre: string;
  direccion: string;
  estado: string;
}

interface UsuarioApp {
  id: number;
  dni: string;
  nombre: string;
  apellido: string;
  rol: RolUsuario;
  estado: EstadoUsuario;
  debeCambiarPassword: boolean;
  clubes: ClubAsignado[];
}

interface LoginResponse {
  token: string;
  usuario: UsuarioApp;
}

interface SocioVista {
  id: number;
  clubId: number;
  nombre: string;
  apellido: string;
  dni: string;
  telefono: string;
  direccion: string;
  emergenciaNombre: string;
  emergenciaTelefono: string;
  emergenciaRelacion: string;
  estado: EstadoSocio;
}

interface ActividadVista {
  id: number;
  clubId: number;
  clubNombre: string;
  nombre: string;
  profesor: string;
  dias: string;
  categoria: string;
  icono: string;
  cupo: number;
  inscriptos: number;
  estado: string;
}

interface CuotaVista {
  id: number;
  clubId: number;
  socioId: number;
  socioNombre: string;
  socioDni: string;
  mes: string;
  importe: number;
  estado: EstadoCuota;
  vencimiento: string;
}

interface AsistenciaVista {
  id: number;
  actividadId: number;
  socioId: number;
  socioNombre: string;
  fecha: string;
  presente: boolean;
}

interface DashboardData {
  sociosActivos: number;
  sociosTotales: number;
  cuotasAlDia: number;
  morosos: number;
  actividadesActivas: number;
  asistenciaMes: number;
  totalCobrado: number;
  movimientos: { descripcion: string; momento: string; tono: string }[];
  proximasActividades: ActividadVista[];
  cuotasPendientes: CuotaVista[];
  sociosRecientes: { id: number; nombre: string; telefono: string; estado: string }[];
  pagosPorMes: { mes: string; valor: number }[];
  asistenciaMensual: { mes: string; valor: number }[];
}

interface ReportesData {
  sociosPorClub: { club: string; socios: number; actividades: number }[];
  pagosPorMes: { mes: string; valor: number }[];
  asistenciaMensual: { mes: string; valor: number }[];
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly apiUrl = 'http://127.0.0.1:8080/api';

  protected loggedIn = signal(false);
  protected activeSection = signal('dashboard');
  protected sidebarOpen = signal(true);
  protected userMenuOpen = signal(false);
  protected loginDni = '';
  protected loginPassword = '';
  protected loginError = signal('');
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
  protected authToken = signal('');
  protected currentUser = signal<UsuarioApp | null>(null);
  protected usuarios = signal<UsuarioApp[]>([]);
  protected clubesDisponibles = signal<ClubAsignado[]>([]);
  protected clubActivoId = signal<number | null>(null);
  protected clubes = signal<ClubVista[]>([]);
  protected socios = signal<SocioVista[]>([]);
  protected actividades = signal<ActividadVista[]>([]);
  protected cuotas = signal<CuotaVista[]>([]);
  protected asistencia = signal<AsistenciaVista[]>([]);
  protected dashboard = signal<DashboardData | null>(null);
  protected reportes = signal<ReportesData | null>(null);
  protected usuarioError = signal('');
  protected socioBusqueda = signal('');
  protected socioEstadoFiltro = signal('TODOS');
  protected socioSeleccionado = signal<SocioVista | null>(null);
  protected socioEditando = signal<SocioVista | null>(null);
  protected socioForm: SocioVista = this.crearSocioVacio();
  protected nuevoSocioForm: SocioVista = this.crearSocioVacio();
  protected selectedActivityId = signal<number | null>(null);
  protected selectedAttendanceDate = '2026-06-03';
  protected selectedMember = signal('TODOS');
  protected dataError = signal('');
  protected nuevaActividad = {
    nombre: '',
    profesor: '',
    dias: '',
    categoria: 'Bienestar',
    icono: 'check',
    cupo: 20,
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

  protected readonly baseNavItems = [
    { id: 'dashboard', label: 'Dashboard', icon: 'grid' },
    { id: 'socios', label: 'Socios', icon: 'users' },
    { id: 'nuevo-socio', label: 'Nuevo socio', icon: 'plus' },
    { id: 'clubes', label: 'Clubes', icon: 'home' },
    { id: 'actividades', label: 'Actividades', icon: 'music' },
    { id: 'cuotas', label: 'Cuotas', icon: 'receipt' },
    { id: 'asistencias', label: 'Asistencias', icon: 'check' },
    { id: 'reportes', label: 'Reportes', icon: 'chart' },
    { id: 'usuarios', label: 'Usuarios', icon: 'key' },
    { id: 'configuracion', label: 'Configuracion', icon: 'settings' },
  ];

  constructor(private http: HttpClient) {
  }

  protected navItems = computed(() => this.baseNavItems.filter((item) => item.id !== 'usuarios' || this.puedeAdministrarUsuarios()));

  protected sectionTitle = computed(() => this.navItems().find((item) => item.id === this.activeSection())?.label ?? 'Dashboard');

  protected sociosFiltrados = computed(() => {
    const busqueda = this.socioBusqueda().trim().toLowerCase();
    const estado = this.socioEstadoFiltro();

    return this.socios().filter((socio) => {
      const coincideEstado = estado === 'TODOS' || socio.estado === estado;
      const texto = `${socio.nombre} ${socio.apellido} ${socio.dni} ${socio.telefono}`.toLowerCase();
      return coincideEstado && (!busqueda || texto.includes(busqueda));
    });
  });

  protected cuotasFiltradas = computed(() => {
    const socioId = this.selectedMember();
    const busqueda = this.socioBusqueda().trim().toLowerCase();
    return this.cuotas().filter((cuota) => {
      const coincideSocio = socioId === 'TODOS' || String(cuota.socioId) === socioId;
      const texto = `${cuota.socioNombre} ${cuota.socioDni} ${cuota.mes} ${cuota.estado}`.toLowerCase();
      return coincideSocio && (!busqueda || texto.includes(busqueda));
    });
  });

  protected metrics = computed(() => {
    const dashboard = this.dashboard();
    return [
      { label: 'Socios activos', value: String(dashboard?.sociosActivos ?? 0), change: `${dashboard?.sociosTotales ?? 0} socios totales` },
      { label: 'Cuotas al dia', value: String(dashboard?.cuotasAlDia ?? 0), change: `${this.formatearImporte(dashboard?.totalCobrado ?? 0)} cobrado` },
      { label: 'Morosos', value: String(dashboard?.morosos ?? 0), change: `${dashboard?.morosos ?? 0} requieren revision`, danger: (dashboard?.morosos ?? 0) > 0 },
      { label: 'Actividades', value: String(dashboard?.actividadesActivas ?? 0), change: `${dashboard?.actividadesActivas ?? 0} en ${this.nombreClubActivo()}` },
    ];
  });

  protected movements = computed(() => this.dashboard()?.movimientos ?? []);
  protected actividadesProximasDelClub = computed(() => this.dashboard()?.proximasActividades ?? []);
  protected cuotasPendientesDelClub = computed(() => this.dashboard()?.cuotasPendientes ?? []);
  protected sociosRecientesDelClub = computed(() => this.dashboard()?.sociosRecientes ?? []);
  protected reportBars = computed(() => this.normalizarBarras(this.dashboard()?.pagosPorMes ?? this.reportes()?.pagosPorMes ?? []));
  protected asistenciaBars = computed(() => this.normalizarBarras(this.dashboard()?.asistenciaMensual ?? this.reportes()?.asistenciaMensual ?? []));
  protected linePoints = computed(() => this.generarLinePoints(this.dashboard()?.asistenciaMensual ?? this.reportes()?.asistenciaMensual ?? []));

  protected nombreClubActivo(): string {
    const clubActivoId = this.clubActivoId();
    return this.clubesDisponibles().find((club) => club.clubId === clubActivoId)?.clubNombre ?? 'Sin club';
  }

  protected login(): void {
    this.loginError.set('');

    this.http.post<LoginResponse>(`${this.apiUrl}/auth/login`, {
      dni: this.loginDni,
      password: this.loginPassword,
    }).subscribe({
      next: (response) => {
        this.authToken.set(response.token);
        this.currentUser.set(response.usuario);
        this.cargarFormularioPerfil(response.usuario);
        this.loggedIn.set(true);
        this.activeSection.set('dashboard');
        this.passwordActual = this.loginPassword;
        this.loginPassword = '';
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
    this.cargarDatosClub();
  }

  protected goTo(section: string): void {
    this.activeSection.set(section);
    this.userMenuOpen.set(false);

    if (section === 'usuarios' && this.puedeAdministrarUsuarios()) {
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

  protected toggleUserMenu(): void {
    this.userMenuOpen.update((open) => !open);
  }

  protected logout(): void {
    this.loggedIn.set(false);
    this.userMenuOpen.set(false);
    this.activeSection.set('dashboard');
    this.authToken.set('');
    this.currentUser.set(null);
    this.usuarios.set([]);
    this.clubesDisponibles.set([]);
    this.clubActivoId.set(null);
    this.clubes.set([]);
    this.socios.set([]);
    this.actividades.set([]);
    this.cuotas.set([]);
    this.asistencia.set([]);
    this.dashboard.set(null);
    this.reportes.set(null);
    this.loginDni = '';
    this.loginPassword = '';
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
    this.http.post<UsuarioApp>(`${this.apiUrl}/auth/cambiar-password`, {
      passwordActual: this.passwordActual,
      nuevaPassword: this.nuevaPassword,
    }, { headers: this.authHeaders() }).subscribe({
      next: (usuario) => {
        this.currentUser.set(usuario);
        this.passwordActual = '';
        this.nuevaPassword = '';
        this.confirmarNuevaPassword = '';
        this.cargarClubes();
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
    this.http.post<UsuarioApp>(`${this.apiUrl}/auth/cambiar-password`, {
      passwordActual: this.configPasswordActual,
      nuevaPassword: this.configNuevaPassword,
    }, { headers: this.authHeaders() }).subscribe({
      next: (usuario) => {
        this.currentUser.set(usuario);
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
    this.http.put<UsuarioApp>(`${this.apiUrl}/auth/me`, {
      nombre: this.perfilNombre,
      apellido: this.perfilApellido,
    }, { headers: this.authHeaders() }).subscribe({
      next: (usuario) => {
        this.currentUser.set(usuario);
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

  protected nombreUsuarioActual(): string {
    const usuario = this.currentUser();
    return usuario ? `${usuario.nombre} ${usuario.apellido}` : 'Usuario';
  }

  protected rolVisible(rol: RolUsuario): string {
    return { SUPERUSUARIO: 'Superusuario', ADMINISTRADOR: 'Administrador', PROFESOR: 'Profesor' }[rol];
  }

  protected cargarUsuarios(): void {
    this.usuarioError.set('');
    this.http.get<UsuarioApp[]>(`${this.apiUrl}/usuarios`, { headers: this.authHeaders() }).subscribe({
      next: (usuarios) => this.usuarios.set(usuarios),
      error: () => this.usuarioError.set('No se pudieron cargar los usuarios. Revisa que el backend este levantado.'),
    });
  }

  protected crearUsuario(): void {
    this.usuarioError.set('');
    this.http.post<UsuarioApp>(`${this.apiUrl}/usuarios`, this.nuevoUsuario, { headers: this.authHeaders() }).subscribe({
      next: () => {
        this.nuevoUsuario = { dni: '', nombre: '', apellido: '', rol: 'PROFESOR', clubId: this.clubActivoId(), rolClub: 'PROFESOR', passwordInicial: '' };
        this.cargarUsuarios();
      },
      error: () => this.usuarioError.set('No se pudo crear el usuario. Revisa los datos y permisos.'),
    });
  }

  protected desactivarUsuario(id: number): void {
    this.usuarioError.set('');
    this.http.delete<UsuarioApp>(`${this.apiUrl}/usuarios/${id}`, { headers: this.authHeaders() }).subscribe({
      next: () => this.cargarUsuarios(),
      error: () => this.usuarioError.set('No se pudo desactivar el usuario.'),
    });
  }

  protected verSocio(socio: SocioVista): void {
    this.socioSeleccionado.set(socio);
  }

  protected editarSocio(socio: SocioVista): void {
    this.socioEditando.set(socio);
    this.socioForm = { ...socio };
  }

  protected guardarSocioEditado(): void {
    const clubId = this.clubActivoId();
    const editando = this.socioEditando();
    if (!clubId || !editando) {
      return;
    }
    this.http.put<SocioVista>(`${this.apiUrl}/clubes/${clubId}/socios/${editando.id}`, this.socioForm, { headers: this.authHeaders() }).subscribe({
      next: () => {
        this.cerrarPanelSocio();
        this.cargarDatosClub();
      },
      error: () => this.dataError.set('No se pudo guardar el socio.'),
    });
  }

  protected guardarNuevoSocio(): void {
    const clubId = this.clubActivoId();
    if (!clubId || !this.nuevoSocioForm.nombre.trim() || !this.nuevoSocioForm.apellido.trim() || !this.nuevoSocioForm.dni.trim()) {
      return;
    }
    this.http.post<SocioVista>(`${this.apiUrl}/clubes/${clubId}/socios`, this.nuevoSocioForm, { headers: this.authHeaders() }).subscribe({
      next: () => {
        this.nuevoSocioForm = this.crearSocioVacio();
        this.cargarDatosClub();
        this.goTo('socios');
      },
      error: () => this.dataError.set('No se pudo crear el socio.'),
    });
  }

  protected alternarEstadoSocio(socio: SocioVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    const actualizado = { ...socio, estado: socio.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO' as EstadoSocio };
    this.http.put<SocioVista>(`${this.apiUrl}/clubes/${clubId}/socios/${socio.id}`, actualizado, { headers: this.authHeaders() }).subscribe({
      next: () => this.cargarDatosClub(),
      error: () => this.dataError.set('No se pudo cambiar el estado del socio.'),
    });
  }

  protected registrarPago(cuota: CuotaVista): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.post<CuotaVista>(`${this.apiUrl}/clubes/${clubId}/cuotas/${cuota.id}/pago`, {}, { headers: this.authHeaders() }).subscribe({
      next: () => this.cargarDatosClub(),
      error: () => this.dataError.set('No se pudo registrar el pago.'),
    });
  }

  protected crearActividad(): void {
    const clubId = this.clubActivoId();
    if (!clubId || !this.nuevaActividad.nombre.trim() || !this.nuevaActividad.profesor.trim() || !this.nuevaActividad.dias.trim()) {
      return;
    }

    this.http.post<ActividadVista>(`${this.apiUrl}/clubes/${clubId}/actividades`, this.nuevaActividad, { headers: this.authHeaders() }).subscribe({
      next: (actividad) => {
        this.nuevaActividad = {
          nombre: '',
          profesor: '',
          dias: '',
          categoria: 'Bienestar',
          icono: 'check',
          cupo: 20,
        };
        this.selectedActivityId.set(actividad.id);
        this.cargarDatosClub();
      },
      error: () => this.dataError.set('No se pudo crear la actividad.'),
    });
  }

  protected seleccionarActividadAsistencia(actividad: ActividadVista): void {
    this.selectedActivityId.set(actividad.id);
    this.goTo('asistencias');
    this.cargarAsistencia();
  }

  protected cargarAsistencia(): void {
    const clubId = this.clubActivoId();
    const actividadId = this.selectedActivityId();
    if (!clubId || !actividadId) {
      this.asistencia.set([]);
      return;
    }
    this.http.get<AsistenciaVista[]>(`${this.apiUrl}/clubes/${clubId}/actividades/${actividadId}/asistencias/${this.selectedAttendanceDate}`, { headers: this.authHeaders() }).subscribe({
      next: (asistencia) => this.asistencia.set(asistencia),
      error: () => this.dataError.set('No se pudo cargar la asistencia.'),
    });
  }

  protected alternarPresente(item: AsistenciaVista, presente: boolean): void {
    this.asistencia.update((asistencias) => asistencias.map((actual) => actual.socioId === item.socioId ? { ...actual, presente } : actual));
  }

  protected guardarAsistencia(): void {
    const clubId = this.clubActivoId();
    const actividadId = this.selectedActivityId();
    if (!clubId || !actividadId) {
      return;
    }
    const payload = this.asistencia().map((item) => ({ socioId: item.socioId, presente: item.presente }));
    this.http.post<AsistenciaVista[]>(`${this.apiUrl}/clubes/${clubId}/actividades/${actividadId}/asistencias/${this.selectedAttendanceDate}`, payload, { headers: this.authHeaders() }).subscribe({
      next: (asistencia) => {
        this.asistencia.set(asistencia);
        this.cargarDashboard();
      },
      error: () => this.dataError.set('No se pudo guardar la asistencia.'),
    });
  }

  protected cerrarPanelSocio(): void {
    this.socioSeleccionado.set(null);
    this.socioEditando.set(null);
    this.socioForm = this.crearSocioVacio();
  }

  protected formatearImporte(importe: number): string {
    return `$ ${new Intl.NumberFormat('es-AR').format(importe)}`;
  }

  protected estadoCuotaVisible(cuota: CuotaVista): string {
    return cuota.estado === 'PAGADO' ? 'Al dia' : 'No al dia';
  }

  protected alturaPagos(valor: number): number {
    const pagos = this.reportes()?.pagosPorMes.map((item) => item.valor) ?? [];
    const max = Math.max(1, ...pagos);
    return valor === 0 ? 4 : Math.max(8, Math.round((valor / max) * 100));
  }

  protected alturaAsistencia(valor: number): number {
    const asistencia = this.reportes()?.asistenciaMensual.map((item) => item.valor) ?? [];
    const max = Math.max(1, ...asistencia);
    return valor === 0 ? 4 : Math.max(8, Math.round((valor / max) * 100));
  }

  private cargarClubes(): void {
    this.http.get<ClubVista[]>(`${this.apiUrl}/clubes`, { headers: this.authHeaders() }).subscribe({
      next: (clubes) => {
        this.clubes.set(clubes);
        const disponibles = clubes.map((club) => ({ clubId: club.id, clubNombre: club.nombre, rol: 'ADMINISTRADOR' as RolClub }));
        this.clubesDisponibles.set(disponibles);
        this.clubActivoId.set(disponibles[0]?.clubId ?? null);
        this.nuevoUsuario.clubId = disponibles[0]?.clubId ?? null;
        this.cargarDatosClub();
      },
      error: () => this.dataError.set('No se pudieron cargar los clubes.'),
    });
  }

  private cargarDatosClub(): void {
    this.dataError.set('');
    this.cargarSocios();
    this.cargarActividades();
    this.cargarCuotas();
    this.cargarDashboard();
    if (this.activeSection() === 'reportes') {
      this.cargarReportes();
    }
  }

  private cargarSocios(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.get<SocioVista[]>(`${this.apiUrl}/clubes/${clubId}/socios`, { headers: this.authHeaders() }).subscribe({
      next: (socios) => this.socios.set(socios),
      error: () => this.dataError.set('No se pudieron cargar los socios.'),
    });
  }

  private cargarActividades(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.get<ActividadVista[]>(`${this.apiUrl}/clubes/${clubId}/actividades`, { headers: this.authHeaders() }).subscribe({
      next: (actividades) => {
        this.actividades.set(actividades);
        if (!this.selectedActivityId() && actividades.length > 0) {
          this.selectedActivityId.set(actividades[0].id);
        }
        if (this.activeSection() === 'asistencias') {
          this.cargarAsistencia();
        }
      },
      error: () => this.dataError.set('No se pudieron cargar las actividades.'),
    });
  }

  private cargarCuotas(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.get<CuotaVista[]>(`${this.apiUrl}/clubes/${clubId}/cuotas`, { headers: this.authHeaders() }).subscribe({
      next: (cuotas) => this.cuotas.set(cuotas),
      error: () => this.dataError.set('No se pudieron cargar las cuotas.'),
    });
  }

  private cargarDashboard(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.get<DashboardData>(`${this.apiUrl}/clubes/${clubId}/dashboard`, { headers: this.authHeaders() }).subscribe({
      next: (dashboard) => this.dashboard.set(dashboard),
      error: () => this.dataError.set('No se pudo cargar el dashboard.'),
    });
  }

  private cargarReportes(): void {
    const clubId = this.clubActivoId();
    if (!clubId) {
      return;
    }
    this.http.get<ReportesData>(`${this.apiUrl}/clubes/${clubId}/reportes`, { headers: this.authHeaders() }).subscribe({
      next: (reportes) => this.reportes.set(reportes),
      error: () => this.dataError.set('No se pudieron cargar los reportes.'),
    });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.authToken()}` });
  }

  private cargarFormularioPerfil(usuario: UsuarioApp): void {
    this.perfilNombre = usuario.nombre;
    this.perfilApellido = usuario.apellido;
  }

  private crearSocioVacio(): SocioVista {
    return {
      id: 0,
      clubId: this.clubActivoId() ?? 0,
      nombre: '',
      apellido: '',
      dni: '',
      telefono: '',
      direccion: '',
      emergenciaNombre: '',
      emergenciaTelefono: '',
      emergenciaRelacion: '',
      estado: 'ACTIVO',
    };
  }

  private normalizarBarras(datos: { valor: number }[]): number[] {
    const max = Math.max(1, ...datos.map((item) => item.valor));
    return datos.map((item) => Math.max(8, Math.round((item.valor / max) * 100)));
  }

  private generarLinePoints(datos: { valor: number }[]): string {
    if (datos.length === 0) {
      return '0,92 294,92';
    }
    const max = Math.max(1, ...datos.map((item) => item.valor));
    return datos.map((item, index) => {
      const x = datos.length === 1 ? 0 : Math.round((index / (datos.length - 1)) * 294);
      const y = Math.max(6, 92 - Math.round((item.valor / max) * 84));
      return `${x},${y}`;
    }).join(' ');
  }
}
