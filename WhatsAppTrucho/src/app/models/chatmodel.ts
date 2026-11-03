import { Mensaje } from './mensajemodel';

export interface Participante {
  id: number;
  nombre: string;
  avatarUrl?: string; // foto de perfil (opcional)
}

export interface Chat {
  id: number;
  codigo: string; // código único del chat (lo genera Back 2)
  participantes: Participante[];
  ultimoMensaje: Mensaje | null;
  noLeidos: number;
  fechaCreacion: string;
}

/** Foto del otro participante (si tiene). */
export function avatarDelChat(chat: Chat, miId: number): string | undefined {
  return chat.participantes.find(p => p.id !== miId)?.avatarUrl;
}

/** Nombre a mostrar: el de los demás participantes. */
export function nombreDelChat(chat: Chat, miId: number): string {
  const otros = chat.participantes.filter(p => p.id !== miId).map(p => p.nombre);
  return otros.length ? otros.join(', ') : 'Chat sin participantes';
}
