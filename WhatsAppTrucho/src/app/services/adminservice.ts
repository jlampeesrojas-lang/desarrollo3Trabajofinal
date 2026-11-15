import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders, HttpParams, HttpResponse, HttpErrorResponse} from "@angular/common/http";
import {Observable, of, throwError} from "rxjs";
import {API, AuthService} from "./authservice";
import {
    ChatAdmin, ClaveChat, EditarUsuario, MensajeAdmin, NuevoUsuario, RegistroAuditoria, UsuarioAdmin
} from "../models/admin";

const USAR_MOCK = true;

@Injectable({providedIn: "root"})
export class AdminService {

    usuariosMock: UsuarioAdmin[] = [
        {id: 1, username: "admin", email: "admin@correo.com", role: "ADMIN", emailVerified: true},
        {id: 2, username: "thomas1", email: "thomas1@correo.com", role: "USER", emailVerified: true},
        {id: 3, username: "laura_m", email: "laura@correo.com", role: "USER", emailVerified: false},
    ];

    auditoriaMock: RegistroAuditoria[] = [
        {
            id: 3,
            username: "thomas1",
            action: "MESSAGE_SENT",
            ip: "181.52.10.4",
            location: "Bogotá, Colombia",
            detail: "TEXTO",
            createdAt: "2026-11-15T10:30:00"
        },
        {
            id: 2,
            username: "laura_m",
            action: "LOGIN_FAIL",
            ip: "190.27.8.2",
            location: "Cali, Colombia",
            detail: null,
            createdAt: "2026-11-15T09:10:00"
        },
        {
            id: 1,
            username: "thomas1",
            action: "LOGIN_OK",
            ip: "181.52.10.4",
            location: "Bogotá, Colombia",
            detail: null,
            createdAt: "2026-11-14T20:00:00"
        },
    ];

    chatsMock: ChatAdmin[] = [
        {
            id: 1,
            code: "CH-7KQ2MZ",
            participants: ["thomas1", "laura_m"],
            lastMessageAt: "2026-11-15T10:30:00",
            messageCount: 2
        },
        {
            id: 2,
            code: "CH-4HD9AX",
            participants: ["thomas1", "admin"],
            lastMessageAt: "2026-11-14T18:00:00",
            messageCount: 2
        },
    ];

    mensajesChat1: MensajeAdmin[] = [
        {
            id: 2, sender: "laura_m", type: "TEXTO", mediaId: null, createdAt: "2026-11-15T10:30:00",
            content: " hola ajjajajajjsja"
        },
        {
            id: 1, sender: "thomas1", type: "TEXTO", mediaId: null, createdAt: "2026-11-15T10:20:00",
            content: "Hola sigma"
        },
    ];

    mensajesChat2: MensajeAdmin[] = [
        {
            id: 4, sender: "admin", type: "TEXTO", mediaId: null, createdAt: "2026-11-14T18:00:00",
            content: "Sí, ya miro haber para solucionarlos"
        },
        {
            id: 3, sender: "thomas1", type: "TEXTO", mediaId: null, createdAt: "2026-11-14T17:50:00",
            content: "¿tienes problemas?"
        },
    ];

    constructor(private http: HttpClient, private auth: AuthService) {
    }

    private headers(): HttpHeaders {
        const token = this.auth.obtenerToken();
        if (token != null) {
            return new HttpHeaders({Authorization: "Bearer " + token});
        }
        return new HttpHeaders();
    }

    listarUsuarios(): Observable<UsuarioAdmin[]> {
        if (USAR_MOCK) {
            return of(this.usuariosMock);
        }
        return this.http.get<UsuarioAdmin[]>(API + "/user/getall", {
            headers: this.headers(),
        });
    }

    crearUsuario(datos: NuevoUsuario): Observable<HttpResponse<string>> {
        if (USAR_MOCK) {
            const nuevo: UsuarioAdmin = {
                id: this.usuariosMock.length + 10,
                username: datos.username,
                email: datos.email,
                role: datos.role,
                emailVerified: true,
            };
            this.usuariosMock.push(nuevo);
            return of(new HttpResponse<string>({status: 201, body: "Usuario creado"}));
        }
        return this.http.post(API + "/user/createjson", datos, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }

    actualizarUsuario(id: number, datos: EditarUsuario): Observable<HttpResponse<string>> {
        if (USAR_MOCK) {
            for (const u of this.usuariosMock) {
                if (u.id === id) {
                    u.username = datos.username;
                    u.role = datos.role;
                }
            }
            return of(new HttpResponse<string>({status: 202, body: "Usuario actualizado"}));
        }
        const params = new HttpParams().set("id", id);
        return this.http.put(API + "/user/updatejson", datos, {
            headers: this.headers(),
            params: params,
            responseType: "text",
            observe: "response",
        });
    }

    eliminarUsuario(id: number): Observable<HttpResponse<string>> {
        if (USAR_MOCK) {
            const quedan: UsuarioAdmin[] = [];
            for (const u of this.usuariosMock) {
                if (u.id !== id) {
                    quedan.push(u);
                }
            }
            this.usuariosMock = quedan;
            return of(new HttpResponse<string>({status: 202, body: "Usuario eliminado"}));
        }
        return this.http.delete(API + "/user/deletebyid/" + id, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }

    listarAuditoria(limite: number): Observable<RegistroAuditoria[]> {
        if (USAR_MOCK) {
            return of(this.auditoriaMock);
        }
        const params = new HttpParams().set("limit", limite);
        return this.http.get<RegistroAuditoria[]>(API + "/admin/audit", {
            headers: this.headers(),
            params: params,
        });
    }

    listarChats(): Observable<ChatAdmin[]> {
        if (USAR_MOCK) {
            return of(this.chatsMock);
        }
        return this.http.get<ChatAdmin[]>(API + "/admin/chats", {
            headers: this.headers(),
        });
    }

    mensajesDeChat(chatId: number, clave: string): Observable<MensajeAdmin[]> {
        if (USAR_MOCK) {
            if (chatId === 1 && clave === "Xk29Lm4QpR7vTb1Z") {
                return of(this.mensajesChat1);
            }
            if (chatId === 2 && clave === "Qw8Er5Ty2Ui0Op3A") {
                return of(this.mensajesChat2);
            }
            return throwError(function () {
                return new HttpErrorResponse({status: 400, error: "Clave incorrecta"});
            });
        }
        return this.http.post<MensajeAdmin[]>(API + "/admin/chats/" + chatId + "/messages", {key: clave}, {
            headers: this.headers(),
        });
    }

    claveDeChat(chatId: number): Observable<ClaveChat> {
        if (USAR_MOCK) {
            if (chatId === 1) {
                return of({key: "Xk29Lm4QpR7vTb1Z"});
            }
            return of({key: "Qw8Er5Ty2Ui0Op3A"});
        }
        return this.http.get<ClaveChat>(API + "/admin/chats/" + chatId + "/key", {
            headers: this.headers(),
        });
    }
}