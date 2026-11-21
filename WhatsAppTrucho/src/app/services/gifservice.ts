import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API, AuthService} from './authservice';
import {Gif} from "../models/gif";

/** Gestiona las consultas de GIF al backend. */
@Injectable({providedIn: 'root'})
export class GifService {
    constructor(private http: HttpClient, private auth: AuthService) {}

    /** Devuelve los GIF que coinciden con el término de búsqueda. */
    buscar(query: string): Observable<Gif[]> {
        const token = this.auth.obtenerToken();
        const headers = token ? new HttpHeaders({Authorization: `Bearer ${token}`}) : new HttpHeaders();
        return this.http.get<Gif[]>(`${API}/gif/buscar`, {headers, params: {query}});
    }
}