import { Injectable } from '@angular/core';

export interface AdjuntoChat { nombre: string; archivo: Blob; }
export interface MensajeChat {
  id: string;
  texto: string;
  enviado: boolean;
  fecha: string;
  adjuntos: AdjuntoChat[];
}
export interface Conversacion { id: string; nombre: string; mensajes: MensajeChat[]; }

@Injectable({ providedIn: 'root' })
export class ChatService {
  private abrir(): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('sigma-chats', 1);
      request.onupgradeneeded = () => request.result.createObjectStore('conversaciones');
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  async cargar(usuario: string): Promise<Conversacion[]> {
    const db = await this.abrir();
    try {
      return await new Promise<Conversacion[]>((resolve, reject) => {
        const request = db.transaction('conversaciones').objectStore('conversaciones').get(usuario);
        request.onsuccess = () => resolve(request.result ?? this.iniciales());
        request.onerror = () => reject(request.error);
      });
    } finally { db.close(); }
  }

  async guardar(usuario: string, chats: Conversacion[]): Promise<void> {
    const db = await this.abrir();
    try {
      await new Promise<void>((resolve, reject) => {
        const tx = db.transaction('conversaciones', 'readwrite');
        tx.objectStore('conversaciones').put(chats, usuario);
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
        tx.onabort = () => reject(tx.error);
      });
    } finally { db.close(); }
  }

  private iniciales(): Conversacion[] {
    return [ ['victor', 'victor', '¡Hola! ¿Cómo estás?'],
      ['sigma-boy', 'Sigma Boy', '¡Hola!'], ['sofia', 'Sofía', '¡Gracias!']
    ].map(([id, nombre, texto]) => ({ id, nombre, mensajes: [{
      id: `${id}-inicial`, texto, enviado: false, fecha: new Date().toISOString(), adjuntos: []
    }] }));
  }
}
