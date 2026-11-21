import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../../services/authservice';
import { Chatsservice } from '../../services/chatsservice';
import { ContactService } from '../../services/contactservice';
import { GifService } from '../../services/gifservice';

import { Contacto } from '../../models/contacto';
import { Gif } from '../../models/gif';

/**
 * Componente encargado de administrar los chats,
 * mensajes, contactos, archivos, cámara y GIFs.
 */
@Component({
    selector: 'app-chatscomponent',
    standalone: false,
    templateUrl: './chatscomponent.html',
    styleUrl: './chatscomponent.css'
})
export class Chatscomponent implements OnInit, OnDestroy {

    contactos: Contacto[] = [];
    seleccionado: Contacto | null = null;

    miUsuario = 'Usuario';
    busquedaChat = '';
    avisoContactos = '';

    mensajes: {
        texto: string;
        enviado: boolean;
        archivo?: File;
        fallo?: boolean;
    }[] = [];

    mensajeNuevo = '';
    adjunto?: File;
    enviando = false;
    avisoMensaje = '';

    private idChat?: number;

    videoCamara?: HTMLVideoElement;
    abriendoCamara = false;

    gifs: Gif[] = [];
    buscandoGif = false;
    avisoGif = '';

    constructor(
        private auth: AuthService,
        private chatsApi: Chatsservice,
        private contactosApi: ContactService,
        private gifsApi: GifService,
        private cdr: ChangeDetectorRef,
        private router: Router
    ) {}

    /**
     * Verifica la sesión y carga los contactos del usuario.
     */
    ngOnInit(): void {

        if (!this.auth.estaLogueado()) {
            this.router.navigate(['/error', '401']);
            return;
        }

        this.miUsuario = this.auth.obtenerUsuario() || 'Usuario';

        this.contactosApi.listarContactos().subscribe({

            next: lista => {
                this.contactos = lista;
                this.cdr.markForCheck();
            },

            error: fallo => {

                if (fallo.status === 401) {
                    this.auth.avisarVencimiento();
                } else {
                    this.avisoContactos = 'No se pudieron cargar los contactos.';
                }

                this.cdr.markForCheck();
            }
        });
    }

    /**
     * Abre una conversación y carga sus mensajes.
     */
    async abrirChat(contacto: Contacto | null) {

        if (this.enviando) {
            return;
        }

        if (this.seleccionado?.id === contacto?.id) {
            return;
        }

        this.cerrarCamara();

        this.seleccionado = contacto;
        this.idChat = undefined;
        this.mensajes = [];
        this.mensajeNuevo = '';
        this.adjunto = undefined;
        this.avisoMensaje = '';

        if (!contacto) {
            return;
        }

        this.enviando = true;

        try {

            await this.cargarMensajes(contacto);

        } catch (error) {

            console.error('Error al cargar mensajes:', error);
            this.avisoMensaje = 'No se pudieron cargar los mensajes.';

        } finally {

            this.enviando = false;
            this.cdr.markForCheck();
        }
    }

    /**
     * Obtiene el historial de mensajes desde el backend.
     */
    private async cargarMensajes(
        contacto: Contacto,
        cargarHistorial = true
    ) {

        const chats = await firstValueFrom(
            this.chatsApi.listarChats()
        );

        this.idChat = chats.find(chat =>
            chat.participantes.some(
                persona => persona.usuario === contacto.usuario
            )
        )?.id;

        if (!cargarHistorial || this.idChat === undefined) {
            return;
        }

        const historial = await firstValueFrom(
            this.chatsApi.obtenerMensajes(this.idChat)
        );

        this.mensajes = historial.reverse().map(mensaje => ({
            texto: mensaje.contenido,
            enviado: mensaje.remitenteNombre === this.miUsuario
        }));
    }

    /**
     * Envía un mensaje a la conversación seleccionada.
     */
    async enviar() {

        const texto = this.mensajeNuevo.trim();
        const archivo = this.adjunto;
        const contacto = this.seleccionado;

        if (!contacto || this.enviando || (!texto && !archivo)) {
            return;
        }

        this.enviando = true;
        this.avisoMensaje = '';

        const mensaje = {
            texto: texto,
            enviado: true,
            archivo: archivo,
            fallo: false
        };

        this.mensajes.push(mensaje);

        this.mensajeNuevo = '';
        this.adjunto = undefined;

        try {

            if (texto) {

                if (this.idChat === undefined) {

                    await this.cargarMensajes(contacto, false);

                    if (this.idChat === undefined) {

                        await firstValueFrom(
                            this.chatsApi.crearChat(contacto.id)
                        );

                        await this.cargarMensajes(contacto, false);
                    }
                }

                if (this.idChat === undefined) {
                    throw new Error('Chat no disponible');
                }

                await firstValueFrom(
                    this.chatsApi.enviarMensaje(this.idChat, texto)
                );
            }

            if (archivo) {
                this.avisoMensaje =
                    'El archivo solo está disponible en este navegador.';
            }

        } catch (error) {

            console.error('Error al enviar mensaje:', error);

            mensaje.fallo = true;
            this.avisoMensaje = 'No se pudo enviar el mensaje.';

        } finally {

            this.enviando = false;
            this.cdr.markForCheck();
        }
    }

    /**
     * Agrega un archivo al mensaje.
     */
    adjuntar(input: HTMLInputElement) {

        if (input.files && input.files.length > 0) {
            this.adjunto = input.files[0];
        }

        input.value = '';
    }

    /**
     * Descarga un archivo adjunto.
     */
    descargar(archivo: File) {

        this.chatsApi.descargar({
            archivo: archivo,
            nombre: archivo.name
        });
    }

    /**
     * Abre la cámara utilizando el servicio.
     */
    async abrirCamara(video: HTMLVideoElement) {

        if (this.abriendoCamara || this.videoCamara) {
            return;
        }

        this.videoCamara = video;
        this.abriendoCamara = true;
        this.avisoMensaje = '';

        try {

            await this.chatsApi.abrirCamara(video);

        } catch (error) {

            console.error('Error al abrir cámara:', error);

            if (this.videoCamara === video) {

                this.cerrarCamara();

                this.avisoMensaje =
                    'No se pudo abrir la cámara. Revisa los permisos.';
            }

        } finally {

            if (!this.videoCamara) {
                this.chatsApi.cerrarCamara(video);
            }

            this.abriendoCamara = false;
            this.cdr.markForCheck();
        }
    }

    /**
     * Captura una fotografía de la cámara.
     */
    tomarFoto(video: HTMLVideoElement) {

        if (video !== this.videoCamara || !video.videoWidth) {
            return;
        }

        const canvas = document.createElement('canvas');

        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        const contexto = canvas.getContext('2d');

        if (!contexto) {
            this.avisoMensaje = 'No se pudo capturar la fotografía.';
            return;
        }

        contexto.drawImage(video, 0, 0);

        canvas.toBlob(foto => {

            if (!foto || video !== this.videoCamara) {
                return;
            }

            this.adjunto = new File(
                [foto],
                'foto-' + Date.now() + '.jpg',
                { type: 'image/jpeg' }
            );

            this.cerrarCamara();
            this.cdr.markForCheck();

        }, 'image/jpeg', 0.85);
    }

    /**
     * Detiene la cámara.
     */
    cerrarCamara() {

        if (!this.videoCamara) {
            return;
        }

        this.chatsApi.cerrarCamara(this.videoCamara);

        this.videoCamara = undefined;
    }

    /**
     * Busca GIFs mediante el servicio.
     */
    buscarGifs(texto: string): void {

        if (!this.auth.estaLogueado()) {
            return;
        }

        const query = texto.trim();

        if (!query || this.buscandoGif) {
            return;
        }

        this.buscandoGif = true;
        this.gifs = [];
        this.avisoGif = '';

        this.gifsApi.buscar(query).subscribe({

            next: lista => {

                this.gifs = lista;

                this.avisoGif = lista.length
                    ? ''
                    : 'No se encontraron GIFs.';

                this.buscandoGif = false;
                this.cdr.markForCheck();
            },

            error: fallo => {

                if (fallo.status === 401) {

                    this.auth.avisarVencimiento();

                } else {

                    this.avisoGif = typeof fallo.error === 'string'
                        ? fallo.error
                        : 'No se pudieron cargar los GIFs.';
                }

                this.buscandoGif = false;
                this.cdr.markForCheck();
            }
        });
    }

    /**
     * Abre la página de contactos.
     */
    irAContactos(): void {

        this.router.navigate(['/contactos']);
    }

    /**
     * Detiene la cámara al destruir el componente.
     */
    ngOnDestroy(): void {

        this.cerrarCamara();
    }
}
