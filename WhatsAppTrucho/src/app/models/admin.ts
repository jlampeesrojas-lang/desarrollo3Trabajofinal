/** Usuario tal como lo muestra el panel de administración. */
export interface UsuarioAdmin {
    id: number;
    username: string;
    email: string;
    role: string;
    emailVerified?: boolean;
}

/** Datos para crear un usuario desde el panel admin. */
export interface NuevoUsuario {
    username: string;
    email: string;
    password: string;
    role: string;
}

/** Datos que se pueden cambiar de un usuario desde el panel admin. */
export interface EditarUsuario {
    username: string;
    password?: string;
    role: string;
}

/** Una fila de la auditoría. */
export interface RegistroAuditoria {
    id: number;
    username: string | null;
    action: string;
    ip: string | null;
    location: string | null;
    detail: string | null;
    createdAt: string;
}

/** Un chat en la lista del panel admin. */
export interface ChatAdmin {
    id: number;
    code: string;
    participants: string[];
    lastMessageAt: string | null;
    messageCount: number;
}

/** Un mensaje ya descifrado que ve el administrador. */
export interface MensajeAdmin {
    id: number;
    sender: string | null;
    type: 'TEXTO' | 'IMAGEN' | 'AUDIO' | 'IA';
    content: string;
    mediaId: number | null;
    createdAt: string;
}

/** Clave secreta de un chat. */
export interface ClaveChat {
    key: string;
}