import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {
    LoginRequest,
    LoginResponse,
    RegistroRequest,
    MessageResponse,
    VerificarCodigoRequest,
    RecuperarRequest,
    CambiarClaveRequest,
} from '../models/authentication';

export const API = 'http://localhost:8080';
const USAR_MOCK = false;

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

@Injectable({providedIn: 'root'})
export class AuthService {
    constructor(private http: HttpClient) {
    }

    login(datos: LoginRequest): Observable<LoginResponse> {
        if (USAR_MOCK) {
            if (datos.identifier === 'admin') {
                return of(RESPUESTA_ADMIN);
            }
            return of(RESPUESTA_USER);
        }
        return this.http.post<LoginResponse>(API + '/auth/login', datos);
    }

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

    confirmarCorreo(token: string): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/confirm-email', {token: token});
    }

    reenviarConfirmacion(email: string): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/resend-confirmation', {email: email});
    }

    pedirCodigoRecuperar(datos: RecuperarRequest): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/recuperar', datos);
    }

    verificarCodigoRecuperar(datos: VerificarCodigoRequest): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/verificar-codigo', datos);
    }

    cambiarClave(datos: CambiarClaveRequest): Observable<MessageResponse> {
        if (USAR_MOCK) {
            return of({message: 'ok'});
        }
        return this.http.post<MessageResponse>(API + '/auth/nueva-clave', datos);
    }

    guardarSesion(respuesta: LoginResponse) {
        localStorage.setItem('token', respuesta.accessToken);
        localStorage.setItem('refreshToken', respuesta.refreshToken);
    }

    obtenerToken(): string | null {
        return localStorage.getItem('token');
    }

    estaLogueado(): boolean {
        if (this.obtenerToken() == null) {
            return false;
        }
        if (this.tokenVencido()) {
            this.cerrarSesion();
            return false;
        }
        return true;
    }

    cerrarSesion() {
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

    leerDatosDelToken(): any {
        const token = this.obtenerToken();
        if (token == null) {
            return null;
        }
        try {
            const partes = token.split('.');
            const texto = partes[1].replace(/-/g, '+').replace(/_/g, '/');
            return JSON.parse(atob(texto));
        } catch (e) {
            return null;
        }
    }

    obtenerRol(): string | null {
        const datos = this.leerDatosDelToken();
        if (datos == null || !datos.role) {
            return null;
        }
        return datos.role;
    }

    obtenerUsuario(): string | null {
        const datos = this.leerDatosDelToken();
        if (datos == null || !datos.sub) {
            return null;
        }
        return datos.sub;
    }

    tokenVencido(): boolean {
        const datos = this.leerDatosDelToken();
        if (datos == null || datos.exp == null) {
            return false;
        }
        return datos.exp * 1000 < Date.now();
    }
}