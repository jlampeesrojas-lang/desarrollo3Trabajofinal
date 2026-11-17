export interface UsuarioAdmin {
    id: number;
    username: string;
    email: string;
    role: string;
    emailVerified?: boolean;
}

export interface NuevoUsuario {
    username: string;
    email: string;
    password: string;
    role: string;
}

export interface EditarUsuario {
    username: string;
    password?: string;
    role: string;
}

export interface RegistroAuditoria {
    id: number;
    username: string | null;
    action: string;
    ip: string | null;
    location: string | null;
    detail: string | null;
    createdAt: string;
}

export interface ChatAdmin {
    id: number;
    code: string;
    participants: string[];
    lastMessageAt: string | null;
    messageCount: number;
}

export interface MensajeAdmin {
    id: number;
    sender: string | null;
    type: 'TEXTO' | 'IMAGEN' | 'AUDIO' | 'IA';
    content: string;
    mediaId: number | null;
    createdAt: string;
}

export interface ClaveChat {
    key: string;
}
