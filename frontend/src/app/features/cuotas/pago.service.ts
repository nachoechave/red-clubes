import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE_URL } from '../../core/config/api.config';
import { MedioPago, PagoVista } from '../../core/models/gestion.models';

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly http = inject(HttpClient);
  registrar(clubId: number, cuotaId: number, medioPago: MedioPago) { return this.http.post<PagoVista>(`${API_BASE_URL}/clubes/${clubId}/cuotas/${cuotaId}/pagos`, { medioPago }); }
  listar(clubId: number, cuotaId: number) { return this.http.get<PagoVista[]>(`${API_BASE_URL}/clubes/${clubId}/cuotas/${cuotaId}/pagos`); }
  anular(clubId: number, cuotaId: number, pagoId: number, motivo: string) { return this.http.post<PagoVista>(`${API_BASE_URL}/clubes/${clubId}/cuotas/${cuotaId}/pagos/${pagoId}/anulacion`, { motivo }); }
}
