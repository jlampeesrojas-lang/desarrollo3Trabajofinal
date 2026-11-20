/** Datos que se envían para iniciar sesión. */
export interface LoginRequest {
    identifier: string;
    password: string;
}

/** Datos que se envían para crear una cuenta. */
export interface RegistroRequest {
    nombreCompleto: string;
    nombreUsuario: string;
    correo: string;
    telefono: string;
    fechaNacimiento: string;
    clave: string;
    confirmarClave: string;
}

/** Datos para verificar el código de recuperación. */
export interface VerificarCodigoRequest {
    correo: string;
    codigo: string;
}

/** Datos para pedir el código de recuperación. */
export interface RecuperarRequest {
    correo: string;
}

/** Datos para cambiar la contraseña con el código recibido. */
export interface CambiarClaveRequest {
    correo: string;
    codigo: string;
    clave: string;
}

/** Respuesta del back al iniciar sesión: tokens y datos de la sesión. */
export interface LoginResponse {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresInSeconds: number;
    username: string;
    role: string;
}

/** Respuesta simple del back con un mensaje. */
export interface MessageResponse {
    message: string;
}