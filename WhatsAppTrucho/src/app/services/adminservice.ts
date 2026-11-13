import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { API, AuthService } from "./authservice";
import { EditarUsuario, NuevoUsuario, RegistroAuditoria, UsuarioAdmin } from "../models/admin";

@Injectable({ providedIn: "root" })
export class AdminService {
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
        return this.http.get<UsuarioAdmin[]>(API + "/user/getall", {
            headers: this.headers(),
        });
    }

    crearUsuario(datos: NuevoUsuario): Observable<HttpResponse<string>> {
        return this.http.post(API + "/user/createjson", datos, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }

    actualizarUsuario(id: number, datos: EditarUsuario): Observable<HttpResponse<string>> {
        const params = new HttpParams().set("id", id);
        return this.http.put(API + "/user/updatejson", datos, {
            headers: this.headers(),
            params: params,
            responseType: "text",
            observe: "response",
        });
    }

    eliminarUsuario(id: number): Observable<HttpResponse<string>> {
        return this.http.delete(API + "/user/deletebyid/" + id, {
            headers: this.headers(),
            responseType: "text",
            observe: "response",
        });
    }

    listarAuditoria(limite: number): Observable<RegistroAuditoria[]> {
        const params = new HttpParams().set("limit", limite);
        return this.http.get<RegistroAuditoria[]>(API + "/admin/audit", {
            headers: this.headers(),
            params: params,
        });
    }
}