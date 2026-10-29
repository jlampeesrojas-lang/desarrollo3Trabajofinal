import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {
    LoginRequest,
    LoginResponse,
    RegistroRequest,
    VerificarCodigoRequest,
    RecuperarRequest,
    CambiarClaveRequest,
    MensajeResponse,
} from '../models/authentication';

const API = 'http://localhost:8080/api';
const USAR_MOCK = true;
const TOKEN_FALSO = 'x.' + btoa('{"rol":"USER"}') + '.x';
const TOKEN_ADMIN = 'x.' + btoa('{"role":"ADMIN"}') + '.x';

@Injectable({providedIn: 'root'})
export class AuthService {
    constructor(private http: HttpClient) {
    }

    login(datos: LoginRequest): Observable<LoginResponse> {
        if (USAR_MOCK) {
            if (datos.usuario === 'admin') {
                return of({token: TOKEN_ADMIN});
            }
            return of({token: TOKEN_FALSO});
        }
        return this.http.post<LoginResponse>(API + '/auth/login', datos);
    }

    registro(datos: RegistroRequest): Observable<MensajeResponse> {
        if (USAR_MOCK) {
            return of({ok: true});
        }
        return this.http.post<MensajeResponse>(API + '/auth/registro', datos);
    }

    verificarRegistro(datos: VerificarCodigoRequest): Observable<MensajeResponse> {
        if (USAR_MOCK) {
            return of({ok: true});
        }
        return this.http.post<MensajeResponse>(API + '/auth/verificar-registro', datos);
    }

    pedirCodigoRecuperar(datos: RecuperarRequest): Observable<MensajeResponse> {
        if (USAR_MOCK) {
            return of({ok: true});
        }
        return this.http.post<MensajeResponse>(API + '/auth/recuperar', datos);
    }

    verificarCodigoRecuperar(datos: VerificarCodigoRequest): Observable<MensajeResponse> {
        if (USAR_MOCK) {
            return of({ok: true});
        }
        return this.http.post<MensajeResponse>(API + '/auth/verificar-codigo', datos);
    }

    cambiarClave(datos: CambiarClaveRequest): Observable<MensajeResponse> {
        if (USAR_MOCK) {
            return of({ok: true});
        }
        return this.http.post<MensajeResponse>(API + '/auth/nueva-clave', datos);
    }

    guardarToken(token: string) {
        localStorage.setItem('token', token);
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
        localStorage.removeItem('token');
    }

    obtenerRol(): string | null {
        const datos = this.leerDatosDelToken();
        if (datos == null) {
            return null;
        }
        if (datos.role) {
            return datos.role;
        }
        if (datos.rol) {
            return datos.rol;
        }
        return null;
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

    tokenVencido(): boolean {
        const datos = this.leerDatosDelToken();
        if (datos == null || datos.exp == null) {
            return false;
        }
        return datos.exp * 1000 < Date.now();
    }
}