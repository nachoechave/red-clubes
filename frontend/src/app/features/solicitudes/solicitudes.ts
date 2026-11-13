import { Component, DestroyRef, effect, inject, input, output, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { API_BASE_URL } from '../../core/config/api.config';

interface Datos { nombre: string; apellido: string; dni: string; email: string; telefono: string; }
interface Solicitud { id: number; referencia: string; datos: Datos; estado: Estado; fechaCreacion: string; fechaRevision: string | null; motivo: string | null; socioId: number | null; }
type Estado = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
interface Pagina { contenido: Solicitud[]; pagina: number; totalPaginas: number; total: number; }

@Component({
  selector: 'app-solicitudes', imports: [CommonModule, FormsModule],
  templateUrl: './solicitudes.html', styleUrl: './solicitudes.css',
})
export class SolicitudesComponent {
  readonly clubId = input<number | null>(null);
  readonly aprobada = output<void>();
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;
  private readonly destroyRef = inject(DestroyRef);
  private revisionCarga = 0;
  protected readonly estados: { valor: Estado; texto: string }[] = [
    { valor: 'PENDIENTE', texto: 'Pendientes' }, { valor: 'APROBADA', texto: 'Aprobadas' }, { valor: 'RECHAZADA', texto: 'Rechazadas' },
  ];
  protected estado = signal<Estado>('PENDIENTE');
  protected pagina = signal<Pagina>({ contenido: [], pagina: 0, totalPaginas: 0, total: 0 });
  protected seleccion = signal<Solicitud | null>(null);
  protected cargando = signal(false);
  protected guardando = signal(false);
  protected error = signal('');
  protected mensaje = signal('');
  protected datos: Datos = { nombre: '', apellido: '', dni: '', email: '', telefono: '' };
  protected motivo = '';

  constructor() {
    effect(() => {
      this.clubId();
      untracked(() => { this.seleccion.set(null); this.mensaje.set(''); this.cargar(0); });
    });
  }
  protected filtrar(estado: Estado): void { this.estado.set(estado); this.seleccion.set(null); this.cargar(0); }
  protected cargar(numero: number): void {
    const club = this.clubId(); const revision = ++this.revisionCarga;
    this.error.set(''); this.pagina.set({ contenido: [], pagina: numero, totalPaginas: 0, total: 0 });
    this.cargando.set(false);
    if (!club) return;
    this.cargando.set(true);
    this.http.get<Pagina>(`${this.base}/clubes/${club}/solicitudes`, { params: { estado: this.estado(), pagina: numero } })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: pagina => { if (revision === this.revisionCarga) { this.pagina.set(pagina); this.cargando.set(false); } },
        error: () => { if (revision === this.revisionCarga) { this.error.set('No se pudieron cargar las solicitudes. Volvé a intentar.'); this.cargando.set(false); } },
      });
  }
  protected revisar(s: Solicitud): void { this.seleccion.set(s); this.datos = { ...s.datos }; this.motivo = ''; this.mensaje.set(''); this.error.set(''); }
  protected resolver(accion: 'aprobar' | 'rechazar'): void {
    const club = this.clubId(); const solicitud = this.seleccion();
    if (!club || !solicitud || this.guardando()) return;
    if (accion === 'rechazar' && !this.motivo.trim()) { this.error.set('Escribí el motivo del rechazo.'); return; }
    this.guardando.set(true); this.error.set('');
    this.http.post<Solicitud>(`${this.base}/clubes/${club}/solicitudes/${solicitud.id}/${accion}`,
      accion === 'aprobar' ? this.datos : { motivo: this.motivo.trim() })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => {
          this.guardando.set(false);
          if (this.clubId() !== club) return;
          this.seleccion.set(null);
          this.mensaje.set(accion === 'aprobar' ? 'Solicitud aprobada. El socio ya está dado de alta.' : 'Solicitud rechazada.');
          if (accion === 'aprobar') this.aprobada.emit();
          this.cargar(0);
        },
        error: error => { this.guardando.set(false); if (this.clubId() === club) this.error.set(error.error?.message ?? 'No se pudo resolver la solicitud. Revisá los datos y volvé a intentar.'); },
      });
  }
}
