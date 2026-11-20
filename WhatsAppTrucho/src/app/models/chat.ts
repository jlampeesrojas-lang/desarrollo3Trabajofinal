export type EstadoEnvio = 'ENVIANDO' | 'ENVIADO' | 'ERROR';

export type EstadoConexion = 'CONECTADO' | 'RECONECTANDO';

/** Persona que participa en un chat. */
export interface Participante {
    id: number;
    nombre: string;
    avatarUrl?: string;
}

/** Mensaje de un chat. */
export interface Mensaje {
    id: string;
    chatId: number;
    remitenteId: number;
    remitenteNombre: string;
    contenido: string;
    fechaEnvio: string;
    estado: EstadoEnvio;
}

/** Conversación con sus participantes y mensajes. */
export interface Chat {
    id: number;
    codigo: string;
    participantes: Participante[];
    ultimoMensaje: Mensaje | null;
    noLeidos: number;
}