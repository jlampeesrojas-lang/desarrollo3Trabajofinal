import { InjectionToken } from '@angular/core';
import { Observable } from 'rxjs';
import { Chat, Participante } from '../models/chatmodel';
import { Mensaje, PaginaMensajes } from '../models/mensajemodel';

/** Contrato que usan los componentes. Hay dos implementaciones: Mock y Http. */
export abstract class ChatApi {
  abstract listarChats(): Observable<Chat[]>;
  abstract obtenerChat(id: number): Observable<Chat>;
  abstract listarMensajes(chatId: number, page: number, size: number): Observable<PaginaMensajes>;
  abstract enviarMensaje(chatId: number, contenido: string): Observable<Mensaje>;
  abstract marcarLeido(chatId: number): Observable<void>;
  /** Contactos con los que se puede iniciar un chat (los administra Front 1 / Back 2). */
  abstract listarContactos(): Observable<Participante[]>;
  /** Crea un chat 1 a 1 con un contacto. Si ya existe, devuelve el existente. */
  abstract crearChat(contactoId: number): Observable<Chat>;
}

/** Id y nombre del usuario en sesión (salen del token de AuthService, de Front 1). */
export const CHAT_USUARIO_ID = new InjectionToken<number>('CHAT_USUARIO_ID');
export const CHAT_USUARIO_NOMBRE = new InjectionToken<string>('CHAT_USUARIO_NOMBRE');
