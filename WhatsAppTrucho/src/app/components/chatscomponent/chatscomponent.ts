import {
    Component,
    OnInit,
    OnDestroy,
    ViewChild,
    ElementRef
} from '@angular/core';

import { AuthService } from '../../services/authservice';
import { Chatsservice } from '../../services/chatsservice';

@Component({
    selector: 'app-chatscomponent',
    standalone: false,
    templateUrl: './chatscomponent.html',
    styleUrl: './chatscomponent.css'
})
export class Chatscomponent implements OnInit, OnDestroy {

    chats: any[] = [
        {
            id: '1',
            nombre: 'Mia',
            mensajes: [
                {
                    id: '1',
                    texto: 'Holaaa, ¿cómo estás?',
                    enviado: false,
                    fecha: new Date(),
                    adjuntos: []
                },
                {
                    id: '2',
                    texto: 'Holaa, todo bien ¿y tú?',
                    enviado: true,
                    fecha: new Date(),
                    adjuntos: []
                },
                {
                    id: '3',
                    texto: 'Bien jajaja, ¿qué haces?',
                    enviado: false,
                    fecha: new Date(),
                    adjuntos: []
                }
            ]
        },
        {
            id: '2',
            nombre: 'Juan',
            mensajes: [
                {
                    id: '4',
                    texto: 'Parce, ¿ya hiciste el proyecto?',
                    enviado: false,
                    fecha: new Date(),
                    adjuntos: []
                },
                {
                    id: '5',
                    texto: 'Todavía me falta terminarlo',
                    enviado: true,
                    fecha: new Date(),
                    adjuntos: []
                },
                {
                    id: '6',
                    texto: 'Dale, me avisas',
                    enviado: false,
                    fecha: new Date(),
                    adjuntos: []
                }
            ]
        },
        {
            id: '3',
            nombre: 'Carlos',
            mensajes: [
                {
                    id: '7',
                    texto: 'Ey, ¿vamos a jugar?',
                    enviado: false,
                    fecha: new Date(),
                    adjuntos: []
                },
                {
                    id: '8',
                    texto: 'Dale, en un rato',
                    enviado: true,
                    fecha: new Date(),
                    adjuntos: []
                }
            ]
        }
    ];

    cargando = true;
    enviando = false;
    error = '';
    seleccionado = '1';
    busqueda = '';
    usuario = '';

    borradores: any = {};

    camaraAbierta = false;
    abriendoCamara = false;

    @ViewChild('videoCamara')
    videoCamara!: ElementRef<HTMLVideoElement>;

    constructor(
        private auth: AuthService,
        private servicio: Chatsservice
    ) {}

    ngOnInit() {
        this.usuario = this.auth.obtenerUsuario() || 'Usuario';
        this.cargando = false;
    }

    // OBTENER CHAT SELECCIONADO

    get chat() {

        for (let i = 0; i < this.chats.length; i++) {

            if (this.chats[i].id === this.seleccionado) {
                return this.chats[i];
            }
        }

        return null;
    }

    // BUSCAR CONVERSACIONES

    get filtrados() {

        return this.chats.filter(c =>
            c.nombre.toLowerCase().includes(this.busqueda.toLowerCase())
        );
    }

    // CAMBIAR CONVERSACION

    seleccionarChat(id: string) {
        this.cerrarCamara();
        this.seleccionado = id;
        this.error = '';
    }

    // BORRADOR

    get borrador() {

        if (!this.borradores[this.seleccionado]) {

            this.borradores[this.seleccionado] = {
                texto: '',
                archivos: []
            };
        }

        return this.borradores[this.seleccionado];
    }

    // ULTIMO MENSAJE

    ultimoMensaje(conversacion: any) {

        if (conversacion.mensajes.length === 0) {
            return 'Sin mensajes';
        }

        let mensajes = conversacion.mensajes;
        let ultimo = mensajes[mensajes.length - 1];

        return ultimo.texto || 'Archivo adjunto';
    }

    // ENVIAR MENSAJE

    enviar() {

        let conversacion = this.chat;

        if (conversacion === null || this.enviando) {
            return;
        }

        let texto = this.borrador.texto.trim();

        if (texto === '' && this.borrador.archivos.length === 0) {
            return;
        }

        this.enviando = true;

        let adjuntos: any[] = [];

        for (let archivo of this.borrador.archivos) {

            adjuntos.push({
                nombre: archivo.name,
                archivo: archivo
            });
        }

        let mensaje = {
            id: Date.now().toString() + Math.random().toString(),
            texto: texto,
            enviado: true,
            fecha: new Date(),
            adjuntos: adjuntos
        };

        conversacion.mensajes.push(mensaje);

        this.borradores[this.seleccionado] = {
            texto: '',
            archivos: []
        };

        this.error = '';
        this.enviando = false;
    }

    // ADJUNTAR ARCHIVOS

    adjuntar(event: Event) {

        let input = event.target as HTMLInputElement;

        if (input.files) {

            for (let i = 0; i < input.files.length; i++) {
                this.borrador.archivos.push(input.files[i]);
            }
        }

        input.value = '';
    }

    quitarArchivo(posicion: number) {
        this.borrador.archivos.splice(posicion, 1);
    }


    private async esperarVideoListo(video: HTMLVideoElement) {

        let intentos = 0;

        while (
            (video.videoWidth === 0 || video.videoHeight === 0 || video.readyState < 2)
            && intentos < 50
            ) {
            await new Promise(resolve => setTimeout(resolve, 100));
            intentos++;
        }

        if (video.videoWidth === 0 || video.videoHeight === 0) {
            throw new Error('La cámara todavía no entrega imagen');
        }
    }

    // ABRIR CAMARA

    async abrirCamara() {

        if (this.camaraAbierta || this.abriendoCamara) {
            return;
        }

        this.abriendoCamara = true;
        this.camaraAbierta = true;
        this.error = '';

        try {

            await this.servicio.abrirCamara(
                this.videoCamara.nativeElement
            );

        } catch (error) {

            console.error('Error de cámara:', error);

            this.cerrarCamara();

            if (error instanceof Error) {
                this.error = 'Error de cámara: ' +
                    error.name + ' - ' + error.message;
            } else {
                this.error = 'No se pudo abrir la cámara';
            }

        } finally {
            this.abriendoCamara = false;
        }

    }

    // TOMAR FOTO Y ENVIAR AL CHAT


    async tomarFoto() {

        console.log('Botón tomar foto presionado');

        if (this.enviando) {
            return;
        }

        let conversacion = this.chat;

        if (!conversacion) {
            this.error = 'No hay una conversación seleccionada';
            return;
        }

        let video = this.videoCamara.nativeElement;

        if (video.videoWidth === 0 || video.videoHeight === 0) {
            this.error = 'La cámara todavía no está lista';
            return;
        }

        this.enviando = true;

        try {

            let canvas = document.createElement('canvas');

            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;

            let contexto = canvas.getContext('2d');

            if (!contexto) {
                throw new Error('No se pudo capturar la imagen');
            }

            contexto.drawImage(video, 0, 0);

            let blob = await new Promise<Blob | null>(resolve => {
                canvas.toBlob(resolve, 'image/jpeg', 0.85);
            });

            if (!blob) {
                throw new Error('No se pudo crear la foto');
            }

            let foto = new File(
                [blob],
                'foto-' + Date.now() + '.jpg',
                { type: 'image/jpeg' }
            );

            conversacion.mensajes.push({
                id: Date.now().toString() + Math.random(),
                texto: '',
                enviado: true,
                fecha: new Date(),
                adjuntos: [
                    {
                        nombre: foto.name,
                        archivo: foto
                    }
                ]
            });

            this.cerrarCamara();
            this.error = '';

            console.log('Foto agregada al chat');

        } catch (error) {

            console.error('Error al tomar foto:', error);

            this.error = 'No se pudo tomar la fotografía';

        } finally {

            this.enviando = false;
        }
    }
//aca

    // CERRAR CAMARA

    cerrarCamara() {

        this.servicio.cerrarCamara(
            this.videoCamara?.nativeElement
        );

        this.camaraAbierta = false;
    }

    // DESCARGAR ARCHIVOS

    descargar(adjunto: any) {
        this.servicio.descargar(adjunto);
    }

    // CERRAR CAMARA AL SALIR

    ngOnDestroy() {
        this.cerrarCamara();
    }
}
