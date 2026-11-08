import { Injectable } from "@angular/core";
import { HttpClient, HttpParams, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { API } from "./authservice";
import { EditarUsuario, NuevoUsuario, RegistroAuditoria, UsuarioAdmin } from "../models/admin";

@Injectable({ providedIn: "root" })
export class AdminService {
    constructor(private http: HttpClient) {
    }

    listarUsuarios(): Observable<UsuarioAdmin[]> {
        return this.http.get<UsuarioAdmin[]>(API + "/user/getall");
    }

    crearUsuario(datos: NuevoUsuario): Observable<HttpResponse<string>> {
        return this.http.post(API + "/user/createjson", datos, {
            responseType: "text",
            observe: "response",
        });
    }

    actualizarUsuario(id: number, datos: EditarUsuario): Observable<HttpResponse<string>> {
        const params = new HttpParams().set("id", id);
        return this.http.put(API + "/user/updatejson", datos, {
            params: params,
            responseType: "text",
            observe: "response",
        });
    }

    eliminarUsuario(id: number): Observable<HttpResponse<string>> {
        return this.http.delete(API + "/user/deletebyid/" + id, {
            responseType: "text",
            observe: "response",
        });
    }

    listarAuditoria(limite: number): Observable<RegistroAuditoria[]> {
        const params = new HttpParams().set("limit", limite);
        return this.http.get<RegistroAuditoria[]>(API + "/admin/audit", {params: params});
    }
}