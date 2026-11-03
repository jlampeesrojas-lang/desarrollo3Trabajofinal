import { HttpClient } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Chat, Participante } from '../models/chatmodel';
import { Mensaje, PaginaMensajes } from '../models/mensajemodel';
import { ChatApi } from './chatapi';

export const CHAT_API_URL = new InjectionToken<string>('CHAT_API_URL');

/**
 * Implementación REST real (Back 2).
 * OJO: las rutas y los nombres de campos son SUPUESTOS. Hay que conciliarlos
 * con el contrato de endpoints de Back 2 antes del 8 de nov.
 */
@Injectable()
export class ChatHttpService extends ChatApi {
  private readonly http = inject(HttpClient);
  private readonly base = inject(CHAT_API_URL);

  listarChats(): Observable<Chat[]> {
    return this.http.get<Chat[]>(`${this.base}/chats`);
  }

  obtenerChat(id: number): Observable<Chat> {
    return this.http.get<Chat>(`${this.base}/chats/${id}`);
  }

  listarMensajes(chatId: number, page: number, size: number): Observable<PaginaMensajes> {
    return this.http
      .get<{ content: Omit<Mensaje, 'estado'>[]; number: number; size: number; totalPages: number }>(
        `${this.base}/chats/${chatId}/messages`, { params: { page, size } })
      .pipe(map(r => ({
        // el servidor entrega lo más reciente primero; la UI lo quiere cronológico
        items: [...r.content].reverse().map(m => ({ ...m, estado: 'ENVIADO' as const })),
        page: r.number, size: r.size, totalPages: r.totalPages,
      })));
  }

  enviarMensaje(chatId: number, contenido: string): Observable<Mensaje> {
    return this.http
      .post<Omit<Mensaje, 'estado'>>(`${this.base}/chats/${chatId}/messages`, { contenido, tipo: 'TEXTO' })
      .pipe(map(m => ({ ...m, estado: 'ENVIADO' as const })));
  }

  marcarLeido(chatId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/chats/${chatId}/read`, {});
  }

  listarContactos(): Observable<Participante[]> {
    return this.http.get<Participante[]>(`${this.base}/contacts`);
  }

  crearChat(contactoId: number): Observable<Chat> {
    return this.http.post<Chat>(`${this.base}/chats`, { contactoId });
  }
}
