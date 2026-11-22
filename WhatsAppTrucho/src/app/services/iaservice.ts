import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {API} from './authservice';
import {BloqueoChatService} from "./bloqueochatservice";

/** Abre la conversación privada del usuario y envía preguntas a la IA. */
@Injectable({
    providedIn: 'root'
})
export class Iaservice {

    constructor(
        private http: HttpClient,
        private bloqueo: BloqueoChatService
    ) {
    }

    /** Incluye el token de la sesión en las peticiones al backend. */
    private headers(): HttpHeaders {
        return this.bloqueo.headers();
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

    /** Pide a la IA que responda al último adjunto o GIF enviado al chat, con el texto opcional que lo acompaña. */
    responder(chatId: number, contenido = '') {
        return this.http.post(
            `${API}/ai/reply/${chatId}`,
            {contenido},
            {headers: this.headers(), responseType: 'text'}
        );
    }
}