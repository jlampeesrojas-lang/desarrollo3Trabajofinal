import {Injectable} from "@angular/core";
import {HttpClient, HttpErrorResponse, HttpHeaders, HttpResponse} from "@angular/common/http";
import {Observable, of, throwError} from "rxjs";
import {API, AuthService} from "./authservice";
import {Contacto, NuevoContacto} from "../models/contacto";

const USAR_MOCK = false;

/** Servicio que llama a los endpoints de contactos. */
@Injectable({providedIn: "root"})
export class ContactService {
    usuariosMock = [
        {id: 1, usuario: "admin", email: "admin@correo.com"},
        {id: 2, usuario: "thomas1", email: "thomas1@correo.com"},
        {id: 3, usuario: "laura_m", email: "laura@correo.com"},
        {id: 4, usuario: "pedro_g", email: "pedro@correo.com"},
        {id: 5, usuario: "normaluser", email: "normal@correo.com"},
    ];

    contactosMock: Contacto[] = [
        {id: 3, usuario: "laura_m", nombre: "Lau", alias: "Lau"},
        {id: 4, usuario: "pedro_g", nombre: "pedro_g", alias: null},
    ];

    constructor(private http: HttpClient, private auth: AuthService) {
    }

    /** Arma el header con el token de la sesión. */
    private headers(): HttpHeaders {
        const token = this.auth.obtenerToken();
        if (token != null) {
            return new HttpHeaders({Authorization: `Bearer ${token}`});
        }
        return new HttpHeaders();
    }

    /** Simula un error del back (mismos códigos y mensajes que ContactController). */
    private falla(status: number, mensaje: string): Observable<never> {
        return throwError(() => new HttpErrorResponse({status: status, error: mensaje}));
    }

    /** Pide al back la lista de contactos del usuario. */
    listarContactos(): Observable<Contacto[]> {
        if (USAR_MOCK) {
            return of(this.contactosMock);
        }
        return this.http.get<Contacto[]>(`${API}/contacts/getall`, {
            headers: this.headers(),
        });
    }

    /** Agrega un contacto por nombre de usuario o correo. En mock valida lo mismo que el back. */
    agregarContacto(datos: NuevoContacto): Observable<HttpResponse<string>> {
        if (USAR_MOCK) {
            const buscado = datos.usuario.trim();
            let encontrado = null;
            for (const u of this.usuariosMock) {
                if (u.usuario === buscado || u.email === buscado.toLowerCase()) {
                    encontrado = u;
                }
            }

            if (encontrado == null) {
                return this.falla(404, "No existe un usuario con ese nombre o correo");
            }
            if (encontrado.usuario === this.auth.obtenerUsuario()) {
                return this.falla(400, "No puedes agregarte a ti mismo");
            }
            for (const c of this.contactosMock) {
                if (c.id === encontrado.id) {
                    return this.falla(406, "Ese usuario ya está en tus contactos");
                }
            }

            let alias: string | null = null;
            if (datos.alias != null && datos.alias.trim() !== "") {
                alias = datos.alias.trim();
            }
            let nombre = encontrado.usuario;
            if (alias != null) {
                nombre = alias;
            }
            this.contactosMock.push({
                id: encontrado.id,
                usuario: encontrado.usuario,
                nombre: nombre,
                alias: alias,
            });
            return of(new HttpResponse<string>({status: 201, body: "Contacto agregado exitosamente"}));
        }
        return this.http.post(`${API}/contacts/createjson`, datos, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }

    /** Elimina un contacto. El id es el del usuario contacto (el mismo que trae la lista). */
    eliminarContacto(id: number): Observable<HttpResponse<string>> {
        if (USAR_MOCK) {
            const quedan: Contacto[] = [];
            for (const c of this.contactosMock) {
                if (c.id !== id) {
                    quedan.push(c);
                }
            }
            this.contactosMock = quedan;
            return of(new HttpResponse<string>({status: 200, body: "Contacto eliminado exitosamente"}));
        }
        return this.http.delete(`${API}/contacts/deletebyid/${id}`, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }
}