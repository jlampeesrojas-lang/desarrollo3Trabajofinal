export type TipoMensaje = 'TEXTO' | 'IMAGEN' | 'AUDIO' | 'IA';
export type EstadoEnvio = 'ENVIANDO' | 'ENVIADO' | 'ERROR';

export interface Mensaje {
  id: number | string;
  chatId: number;
  remitenteId: number;
  remitenteNombre: string;
  contenido: string;
  tipo: TipoMensaje;
  mediaUrl?: string;
  fechaEnvio: string; // ISO 8601
  estado: EstadoEnvio;
}

export interface PaginaMensajes {
  items: Mensaje[]; // en orden cronológico (el más antiguo primero)
  page: number; // 0 = los más recientes
  size: number;
  totalPages: number;
}
