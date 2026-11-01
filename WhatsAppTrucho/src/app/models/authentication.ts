export interface LoginRequest {
    identifier: string;
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
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresInSeconds: number;
    username: string;
    role: string;
}

export interface MessageResponse {
    message: string;
}