export type EstadoEnvio = 'ENVIANDO' | 'ENVIADO' | 'ERROR';

export type EstadoConexion = 'CONECTADO' | 'RECONECTANDO';

export interface Participante {
    id: number;
    nombre: string;
    avatarUrl?: string;
}

export interface Mensaje {
    id: string;
    chatId: number;
    remitenteId: number;
    remitenteNombre: string;
    contenido: string;
    fechaEnvio: string;
    estado: EstadoEnvio;
}

export interface Chat {
    id: number;
    codigo: string;
    participantes: Participante[];
    ultimoMensaje: Mensaje | null;
    noLeidos: number;
}
