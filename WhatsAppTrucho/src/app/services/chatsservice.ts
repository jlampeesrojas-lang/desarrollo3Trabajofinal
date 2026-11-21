import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({
    providedIn: 'root'
})
export class Chatsservice {

    private url = 'http://localhost:8080/api/mensajes';

    constructor(private http: HttpClient) {
    }

    obtenerToken() {
        return localStorage.getItem('token');
    }

    obtenerOpciones() {
        const token = this.obtenerToken();

        let headers = new HttpHeaders();

        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }

        return {headers};
    }

    obtenerMensajes(idChat: string) {
        return this.http.get<unknown[]>(
            this.url + '/' + idChat,
            this.obtenerOpciones()
        );
    }

    enviarMensaje(idChat: string, texto: string) {
        return this.http.post<unknown>(
            `${this.url}/${idChat}`,
            {texto: texto},
            this.obtenerOpciones()
        );
    }

    eliminarMensaje(idMensaje: string) {
        return this.http.delete(
            `${this.url}/eliminar/${idMensaje}`,
            this.obtenerOpciones()
        );
    }


    private flujoCamara: MediaStream | null = null;

    async abrirCamara(video: HTMLVideoElement) {
        this.cerrarCamara(video);

        this.flujoCamara = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: false
        });

        video.srcObject = this.flujoCamara;
        await video.play();
    }

    cerrarCamara(video?: HTMLVideoElement) {
        if (this.flujoCamara) {
            this.flujoCamara.getTracks().forEach(t => t.stop());
            this.flujoCamara = null;
        }

        if (video) {
            video.srcObject = null;
        }
    }

    descargar(adjunto: { archivo: Blob; nombre: string }) {
        const url = URL.createObjectURL(adjunto.archivo);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = adjunto.nombre;
        enlace.click();
        URL.revokeObjectURL(url);
    }


}