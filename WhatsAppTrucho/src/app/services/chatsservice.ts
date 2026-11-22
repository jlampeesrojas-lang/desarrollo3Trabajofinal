import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {catchError, map, of, switchMap, throwError} from 'rxjs';

import {API, AuthService} from './authservice';
import type {Chat, MensajeHistorial} from '../models/chat';
import {BloqueoChatService} from "./bloqueochatservice";

@Injectable({
    providedIn: 'root'
})
export class Chatsservice {

    constructor(
        private http: HttpClient,
        private bloqueo: BloqueoChatService
    ) {
    }

    /** Crea las cabeceras HTTP con el token de acceso de la sesión actual. */
    private headers(): HttpHeaders {
        return this.bloqueo.headers();
    }

    /** Busca una conversación usando el id del usuario contacto. */
    private buscarChat(contactoId: number) {
        return this.http.get<Pick<Chat, 'id' | 'participantes'>[]>(
            `${API}/chats/getall`,
            {headers: this.headers()}
        ).pipe(
            map(chats => chats.find(chat =>
                chat.participantes.some(persona => persona.id === contactoId)
            ))
        );
    }

    /** Obtiene el id del chat existente o crea la conversación. */
    abrirChat(contactoId: number) {
        return this.buscarChat(contactoId).pipe(
            switchMap(chat => {
                if (chat) {
                    return of(chat.id);
                }

                return this.http.post(
                    `${API}/chats/createjson`,
                    {contactoId},
                    {headers: this.headers(), responseType: 'text'}
                ).pipe(
                    catchError(error => {
                        if (error.status === 406) {
                            return of('');
                        }
                        return throwError(() => error);
                    }),
                    switchMap(() => this.buscarChat(contactoId)),
                    map(creado => {
                        if (!creado) {
                            throw new Error('No se encontró el chat creado.');
                        }
                        return creado.id;
                    })
                );
            })
        );
    }

    /** Lee la página más reciente del historial. */
    obtenerMensajes(chatId: number) {
        return this.http.get<MensajeHistorial[]>(
            `${API}/chats/getmessages/${chatId}`,
            {
                headers: this.headers(),
                params: {page: 0, size: 20}
            }
        );
    }

    /** El backend existente devuelve la confirmación como texto. */
    enviarMensaje(chatId: number, contenido: string) {
        return this.http.post<MensajeHistorial>(
            `${API}/chats/sendmessage/${chatId}`,
            {contenido},
            {headers: this.headers()}
        );
    }

    /** Envía al chat uno de los GIF devueltos por el buscador. */
    enviarGif(chatId: number, url: string) {
        return this.http.post(
            `${API}/chats/sendmessage/${chatId}`,
            {tipo: 'GIF', contenido: url},
            {headers: this.headers(), responseType: 'text'}
        );
    }

    /** Sube los bytes del archivo al chat seleccionado. */
    enviarArchivo(chatId: number, archivo: File) {
        const datos = new FormData();
        datos.append('archivo', archivo, archivo.name);

        return this.http.post(
            `${API}/media/chat/${chatId}`,
            datos,
            {headers: this.headers(), responseType: 'text'}
        );
    }

    /** Recupera un adjunto con el token del usuario. */
    obtenerArchivo(ruta: string) {
        return this.http.get(`${API}${ruta}`, {
            headers: this.headers(),
            responseType: 'blob'
        });
    }
}