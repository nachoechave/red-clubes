export type RolUsuario = 'SUPERUSUARIO' | 'ADMINISTRADOR' | 'OPERADOR' | 'PROFESOR';
export type EstadoUsuario = 'ACTIVO' | 'INACTIVO';
export type RolClub = 'ADMINISTRADOR' | 'OPERADOR' | 'PROFESOR';

export interface ClubAsignado {
  clubId: number;
  clubNombre: string;
  logoUrl?: string | null;
  rol: RolClub;
  actividadIds: number[];
}

export interface UsuarioApp {
  id: number;
  dni: string;
  nombre: string;
  apellido: string;
  rol: RolUsuario;
  estado: EstadoUsuario;
  debeCambiarPassword: boolean;
  clubes: ClubAsignado[];
}

export interface LoginRequest {
  dni: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  usuario: UsuarioApp;
}
