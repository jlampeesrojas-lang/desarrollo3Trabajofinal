import {Injectable, signal} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {Observable, of, throwError} from 'rxjs';

import {
    LoginRequest,
    LoginResponse,
    RegistroRequest,
    MessageResponse,
    VerificarCodigoRequest,
    RecuperarRequest,
    CambiarClaveRequest,
} from '../models/authentication';


export const API = 'https://gpcueb.org/trabajofinalback';
const USAR_MOCK = false;
const USAR_MOCK_RECUPERAR = false;

const TOKEN_USER = 'x.' + btoa('{"role":"USER","sub":"usuario"}') + '.x';
const TOKEN_ADMIN = 'x.' + btoa('{"role":"ADMIN","sub":"admin"}') + '.x';

const RESPUESTA_USER: LoginResponse = {
    accessToken: TOKEN_USER,
    refreshToken: 'refresh-falso',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    username: 'usuario',
    role: 'USER',
};

const RESPUESTA_ADMIN: LoginResponse = {
    accessToken: TOKEN_ADMIN,
    refreshToken: 'refresh-falso',
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    username: 'admin',
    role: 'ADMIN',
};

/** Servicio de autenticación: login, registro, confirmación de correo, recuperación y manejo de la sesión. */
@Injectable({providedIn: 'root'})
export class AuthService {
    sesionExpirada = signal(false);
    private reloj?: ReturnType<typeof setTimeout>;

    constructor(private http: HttpClient) {
        this.programarVencimiento();
    }

    /** Envía las credenciales al back y devuelve los tokens de sesión. */
    login(datos: LoginRequest): Observable<LoginResponse> {
        if (USAR_MOCK) {
            if (datos.identifier === 'admin') {
                return of(RESPUESTA_ADMIN);
            }
            return of(RESPUESTA_USER);
        }
        return this.http.post<LoginResponse>(API + '/auth/login', datos);
    }

    /** Envía al back los datos para crear una cuenta. */
    registro(datos: RegistroRequest): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        const cuerpo = {
            username: datos.nombreUsuario,
            email: datos.correo,
            password: datos.clave,
        };
        return this.http.post<MessageResponse>(API + '/auth/register', cuerpo);
    }

    /** Confirma el correo con el token recibido en el enlace. */
    confirmarCorreo(token: string): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/confirm-email', {token: token});
    }

    /** Pide al back reenviar el correo de confirmación. */
    reenviarConfirmacion(email: string): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/resend-confirmation', {email: email});
    }

    /** Pide al back que envíe un código de recuperación al correo. */
    pedirCodigoRecuperar(datos: RecuperarRequest): Observable<MessageResponse> {
        if (USAR_MOCK_RECUPERAR) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/recuperar', datos);
    }

    /** Verifica el código que llegó al correo. En mock el código correcto es 123456. */
    verificarCodigoRecuperar(datos: VerificarCodigoRequest): Observable<MessageResponse> {
        if (USAR_MOCK_RECUPERAR) {
            if (datos.codigo === '123456') {
                return of({message: 'ok'});
            }
            return throwError(() => new HttpErrorResponse({status: 400, error: 'Código incorrecto o vencido'}));
        }
        return this.http.post<MessageResponse>(API + '/auth/verificar-codigo', datos);
    }

    /** Cambia la contraseña usando el correo y el código ya verificado. */
    cambiarClave(datos: CambiarClaveRequest): Observable<MessageResponse> {
        if (USAR_MOCK_RECUPERAR) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/nueva-clave', datos);
    }

    /** Guarda los tokens de la sesión y programa su vencimiento. */
    guardarSesion(respuesta: LoginResponse) {
        localStorage.setItem('token', respuesta.accessToken);
        localStorage.setItem('refreshToken', respuesta.refreshToken);
        this.sesionExpirada.set(false);
        this.programarVencimiento();
    }

    /** Programa el aviso para cuando venza el token de acceso. */
    private programarVencimiento() {
        clearTimeout(this.reloj);
        const datos = this.leerDatosDelToken();
        const token = this.obtenerToken();
        if (!token || !datos?.exp) return;

        const restante = datos.exp * 1000 - Date.now();
        if (restante <= 0) {
            this.avisarVencimiento();
            return;
        }
        this.reloj = setTimeout(() => {
            this.programarVencimiento();
        }, restante);
    }

    /** Cierra la sesión y avisa que el token venció. */
    avisarVencimiento() {
        if (this.obtenerToken()) {
            this.sesionExpirada.set(true);
        }
    }

    /** Devuelve el token de acceso guardado, o null si no hay sesión. */
    obtenerToken(): string | null {
        return localStorage.getItem('token');
    }

    /** Indica si hay una sesión activa con token vigente. */
    estaLogueado(): boolean {
        if (this.obtenerToken() == null) {
            return false;
        }
        if (this.tokenVencido() || this.sesionExpirada()) {
            this.avisarVencimiento();
            return false;
        }
        return true;
    }

    /** Revoca el refresh token en el back y borra la sesión local. */
    cerrarSesion() {
        clearTimeout(this.reloj);
        this.sesionExpirada.set(false);
        const refresh = localStorage.getItem('refreshToken');
        if (refresh != null && !USAR_MOCK) {
            this.http.post(API + '/auth/logout', {refreshToken: refresh}).subscribe({
                error: function () {
                },
            });
        }
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
    }

    /** Lee el contenido (payload) del token de acceso. */
    leerDatosDelToken(): DatosToken | null {
        const token = this.obtenerToken();
        if (token == null) {
            return null;
        }
        try {
            const partes = token.split('.');
            const texto = partes[1].replace(/-/g, '+').replace(/_/g, '/');
            return JSON.parse(atob(texto));
        } catch {
            return null;
        }
    }

    /** Devuelve el rol del usuario de la sesión, o null. */
    obtenerRol(): string | null {
        const datos = this.leerDatosDelToken();
        if (datos == null || !datos.role) {
            return null;
        }
        return datos.role;
    }

    /** Devuelve el nombre del usuario de la sesión, o null. */
    obtenerUsuario(): string | null {
        const datos = this.leerDatosDelToken();
        if (datos == null || !datos.sub) {
            return null;
        }
        return datos.sub;
    }

    /** Indica si el token de acceso ya venció. */
    tokenVencido(): boolean {
        const datos = this.leerDatosDelToken();
        if (datos == null || datos.exp == null) {
            return false;
        }
        return datos.exp * 1000 < Date.now();
    }
}