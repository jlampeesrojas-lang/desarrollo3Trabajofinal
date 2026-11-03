import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, defer, map, timer } from 'rxjs';
import { Chat, Participante } from '../models/chatmodel';
import { Mensaje, PaginaMensajes } from '../models/mensajemodel';
import { CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE, ChatApi } from './chatapi';

const LATENCIA_MS = 350;

@Injectable()
export class ChatMockService extends ChatApi {
  private readonly yo = inject(CHAT_USUARIO_ID);
  private readonly miNombre = inject(CHAT_USUARIO_NOMBRE, { optional: true }) ?? 'Tú';
  static readonly ID_IA = 100;
  private readonly nombres: Record<number, string> = { 100: 'IA' };
  /** Contactos disponibles para crear chats. Vacío por ahora: los traerá Front 1 / Back 2. */
  contactos: Participante[] = [];
  private chats: Chat[] = [];
  private mensajes = new Map<number, Mensaje[]>();
  private idMensaje = 0;

  constructor() {
    super();
    // Único chat de la demo: el asistente de IA.
    const chat: Chat = {
      id: 1, codigo: 'CH-IA0001', noLeidos: 0, ultimoMensaje: null,
      participantes: [
        { id: this.yo, nombre: this.nombreDe(this.yo) },
        { id: ChatMockService.ID_IA, nombre: 'IA', avatarUrl: 'ia.jpg' },
      ],
      fechaCreacion: new Date().toISOString(),
    };
    this.chats.push(chat);
    this.mensajes.set(1, []); // el chat arranca vacío
  }

  listarChats(): Observable<Chat[]> {
    return this.responder(() => this.chats.map(c => ({ ...c })));
  }

  obtenerChat(id: number): Observable<Chat> {
    return this.responder(() => ({ ...this.buscar(id) }));
  }

  listarMensajes(chatId: number, page: number, size: number): Observable<PaginaMensajes> {
    return this.responder(() => {
      this.buscar(chatId);
      const todos = this.mensajes.get(chatId) ?? [];
      const fin = todos.length - page * size;
      const inicio = Math.max(0, fin - size);
      return {
        items: fin > 0 ? todos.slice(inicio, fin).map(m => ({ ...m })) : [],
        page,
        size,
        totalPages: Math.max(1, Math.ceil(todos.length / size)),
      };
    });
  }

  enviarMensaje(chatId: number, contenido: string): Observable<Mensaje> {
    return this.responder(() => {
      const chat = this.buscar(chatId);
      if (contenido.includes('#error')) {
        throw new HttpErrorResponse({ status: 500, statusText: 'Error simulado' });
      }
      const m = this.crear(chatId, this.yo, contenido, new Date().toISOString());
      chat.ultimoMensaje = m;
      return { ...m };
    });
  }

  marcarLeido(chatId: number): Observable<void> {
    return this.responder(() => {
      this.buscar(chatId).noLeidos = 0;
    });
  }

  listarContactos(): Observable<Participante[]> {
    return this.responder(() => this.contactos.map(c => ({ ...c })));
  }

  crearChat(contactoId: number): Observable<Chat> {
    return this.responder(() => {
      const contacto = this.contactos.find(c => c.id === contactoId);
      if (!contacto || contactoId === this.yo) {
        throw new HttpErrorResponse({ status: 400, statusText: 'Contacto no válido' });
      }
      const existente = this.chats.find(c => c.participantes.some(p => p.id === contactoId));
      if (existente) return { ...existente };
      const chat: Chat = {
        id: this.chats.reduce((max, c) => Math.max(max, c.id), 0) + 1,
        codigo: this.codigoUnico(),
        participantes: [
          { id: this.yo, nombre: this.nombreDe(this.yo) },
          { ...contacto },
        ],
        ultimoMensaje: null,
        noLeidos: 0,
        fechaCreacion: new Date().toISOString(),
      };
      this.chats.push(chat);
      this.mensajes.set(chat.id, []);
      return { ...chat };
    });
  }

  // ---------- internos ----------
  private nombreDe(id: number): string {
    if (id === this.yo) return this.miNombre;
    return this.nombres[id] ?? this.contactos.find(c => c.id === id)?.nombre ?? 'Usuario';
  }

  private codigoUnico(): string {
    const letras = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    let codigo: string;
    do {
      codigo = 'CH-' + Array.from({ length: 6 }, () => letras[Math.floor(Math.random() * letras.length)]).join('');
    } while (this.chats.some(c => c.codigo === codigo));
    return codigo;
  }

  private responder<T>(fn: () => T): Observable<T> {
    return defer(() => timer(LATENCIA_MS).pipe(map(fn)));
  }

  private buscar(id: number): Chat {
    const chat = this.chats.find(c => c.id === id);
    if (!chat) throw new HttpErrorResponse({ status: 404, statusText: 'Chat no encontrado' });
    return chat;
  }

  private crear(chatId: number, remitenteId: number, contenido: string, fecha: string): Mensaje {
    const m: Mensaje = {
      id: ++this.idMensaje,
      chatId,
      remitenteId,
      remitenteNombre: this.nombreDe(remitenteId),
      contenido,
      tipo: remitenteId === ChatMockService.ID_IA ? 'IA' : 'TEXTO',
      fechaEnvio: fecha,
      estado: 'ENVIADO',
    };
    const lista = this.mensajes.get(chatId) ?? [];
    lista.push(m);
    this.mensajes.set(chatId, lista);
    return m;
  }
}
