export interface LoginRequest {
    usuario: string;
    password: string;
}

export interface RegistroRequest {
    nombreCompleto: string;
    nombreUsuario: string;
    correo: string;
    telefono: string;
    fechaNacimiento: string;
    clave: string;
    confirmarClave: string;
}

export interface VerificarCodigoRequest {
    correo: string;
    codigo: string;
}

export interface RecuperarRequest {
    correo: string;
}

export interface CambiarClaveRequest {
    correo: string;
    codigo: string;
    clave: string;
}

export interface LoginResponse {
    token: string;
}

export interface MensajeResponse {
    ok?: boolean;
    mensaje?: string;
}