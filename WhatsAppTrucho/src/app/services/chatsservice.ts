import { Contacto } from '../models/contacto';
import { Mensaje } from '../models/chat';
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

    listarChats() {
        return this.http.get<{ id: number; participantes: Contacto[] }[]>(
            'http://localhost:8080/chats/getall', this.obtenerOpciones()
        );
    }

    crearChat(contactoId: number) {
        return this.http.post('http://localhost:8080/chats/createjson', { contactoId }, {
            ...this.obtenerOpciones(), responseType: 'text'
        });
    }

    obtenerMensajes(idChat: number) {
        return this.http.get<Mensaje[]>(
            'http://localhost:8080/chats/getmessages/' + idChat, this.obtenerOpciones()
        );
    }

    enviarMensaje(idChat: number, contenido: string) {
        return this.http.post('http://localhost:8080/chats/sendmessage/' + idChat, { contenido }, {
            ...this.obtenerOpciones(), responseType: 'text'
        });
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