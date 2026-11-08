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