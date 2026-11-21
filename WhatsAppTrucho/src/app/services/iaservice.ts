import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {API, AuthService} from './authservice';

/** Abre la conversación privada del usuario y envía preguntas a la IA. */
@Injectable({
    providedIn: 'root'
})
export class Iaservice {

    constructor(
        private http: HttpClient,
        private auth: AuthService
    ) {
    }

    /** Incluye el token de la sesión en las peticiones al backend. */
    private headers(): HttpHeaders {
        const token = this.auth.obtenerToken();

        if (token) {
            return new HttpHeaders({Authorization: `Bearer ${token}`});
        }

        return new HttpHeaders();
    }

    /** Obtiene el mismo id cada vez que el usuario abre su conversación. */
    abrirChat() {
        return this.http.post<number>(
            `${API}/chats/ia`,
            null,
            {headers: this.headers()}
        );
    }

    /** Guarda la pregunta y devuelve el texto de la respuesta de la IA. */
    preguntar(chatId: number, contenido: string) {
        return this.http.post(
            `${API}/ai/askinchat/${chatId}`,
            {contenido},
            {headers: this.headers(), responseType: 'text'}
        );
    }
}