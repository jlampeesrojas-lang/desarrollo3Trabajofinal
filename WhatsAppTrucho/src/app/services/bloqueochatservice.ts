import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {API, AuthService} from './authservice';

@Injectable({providedIn: 'root'})
export class BloqueoChatService {
    private permiso = '';

    constructor(private http: HttpClient, private auth: AuthService) {
    }

    /** Incluye la sesión y, cuando existe, el permiso temporal de esta pestaña. */
    headers(): HttpHeaders {
        let headers = new HttpHeaders();
        const token = this.auth.obtenerToken();

        if (token) headers = headers.set('Authorization', `Bearer ${token}`);
        if (this.permiso) headers = headers.set('X-Chat-Permiso', this.permiso);

        return headers;
    }

    verClave(id: number, password: string) {
        return this.http.post(`${API}/chats/${id}/clave`, {password},
            {headers: this.headers(), responseType: 'text'});
    }

    desbloquear(id: number, clave: string) {
        return this.http.post(`${API}/chats/${id}/desbloquear`, {clave},
            {headers: this.headers(), responseType: 'text'});
    }

    guardarPermiso(permiso: string): void {
        this.permiso = permiso;
    }

    /** Captura la cabecera antes de borrar el permiso local. */
    cerrar(id: number | null): void {
        const headers = this.headers();
        const habiaPermiso = this.permiso !== '';
        this.permiso = '';

        if (id !== null && habiaPermiso) {
            this.http.post(`${API}/chats/${id}/bloquear`, null, {headers})
                .subscribe({error: () => {}});
        }
    }
}