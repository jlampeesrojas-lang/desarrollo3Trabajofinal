import {firstValueFrom} from 'rxjs';
import {Chatsservice} from '../../services/chatsservice';
import {ContactService} from "../../services/contactservice";
import {Contacto} from "../../models/contacto";
import {GifService} from "../../services/gifservice";
import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../../services/authservice';
import {Gif} from "../../models/gif";

/** Página principal: lista de chats, conversación, adjuntos, GIFs y audio. */
@Component({
    selector: 'app-paginaprincipalcomponent',
    standalone: false,
    styleUrl: './paginaprincipalcomponent.css',
    templateUrl: './paginaprincipalcomponent.html',
})
export class Paginaprincipalcomponent implements OnInit, OnDestroy {
    seccion: 'chats' | 'ia' | 'contactos' = 'chats';
    contactos: Contacto[] = [];
    seleccionado: Contacto | null = null;
    avisoContactos = '';
    miUsuario = 'Usuario';
    busquedaChat = '';
    mensajeNuevo = '';
    adjunto?: File;
    videoCamara?: HTMLVideoElement;
    abriendoCamara = false;
    mensajes: { texto: string; enviado: boolean; archivo?: File; fallo?: boolean }[] = [];
    enviando = false;
    avisoMensaje = '';
    private idChat?: number;
    gifs: Gif[] = [];
    buscandoGif = false;
    avisoGif = '';
    preguntaIA = '';

    busquedaContacto = '';
    mensajeContacto = '';
    errorContacto = '';

    /**
     * Inicializa los servicios de autenticación y navegación.
     */
    constructor(
        private contactosApi: ContactService,
        private auth: AuthService,
        private gifsApi: GifService,
        private cdr: ChangeDetectorRef,
        private router: Router,
        private chatsApi: Chatsservice
    ) {
    }

    /**
     * Obtiene el nombre del usuario al iniciar el componente.
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
                if (fallo.status === 401) this.auth.avisarVencimiento();
                else this.avisoContactos = 'No se pudieron cargar los contactos.';
                this.cdr.markForCheck();
            }
        });
    }

    async abrirChat(contacto: Contacto | null) {
        if (this.enviando || this.seleccionado?.id === contacto?.id) return;
        this.cerrarCamara();
        this.seleccionado = contacto;
        this.idChat = undefined;
        this.mensajes = [];
        this.mensajeNuevo = '';
        this.adjunto = undefined;
        this.avisoMensaje = '';
        if (!contacto) return;

        this.enviando = true;
        try {
            await this.cargarMensajes(contacto);
        } catch {
            this.avisoMensaje = 'No se pudieron cargar los mensajes.';
        } finally {
            this.enviando = false;
            this.cdr.markForCheck();
        }
    }

    private async cargarMensajes(contacto: Contacto, cargarHistorial = true) {
        const chats = await firstValueFrom(this.chatsApi.listarChats());
        this.idChat = chats.find(chat =>
            chat.participantes.some(persona => persona.usuario === contacto.usuario)
        )?.id;
        if (!cargarHistorial || this.idChat === undefined) return;

        const historial = await firstValueFrom(this.chatsApi.obtenerMensajes(this.idChat));
        this.mensajes = historial.reverse().map(mensaje => ({
            texto: mensaje.contenido,
            enviado: mensaje.remitenteNombre === this.miUsuario
        }));
    }

    async abrirCamara(video: HTMLVideoElement) {
        if (this.abriendoCamara || this.videoCamara) return;

        this.videoCamara = video;
        this.abriendoCamara = true;
        this.avisoMensaje = '';
        try {
            await this.chatsApi.abrirCamara(video);
        } catch {
            if (this.videoCamara === video) {
                this.avisoMensaje = 'No se pudo abrir la c\u00e1mara. Revisa el permiso del navegador.';
                this.cerrarCamara();
            }
        } finally {
            if (!this.videoCamara) this.chatsApi.cerrarCamara(video);
            this.abriendoCamara = false;
            this.cdr.markForCheck();
        }
    }

    tomarFoto(video: HTMLVideoElement) {
        if (video !== this.videoCamara || !video.videoWidth) return;

        const lienzo = document.createElement('canvas');
        lienzo.width = video.videoWidth;
        lienzo.height = video.videoHeight;
        lienzo.getContext('2d')?.drawImage(video, 0, 0);
        lienzo.toBlob(foto => {
            if (!foto || video !== this.videoCamara) return;
            this.adjunto = new File([foto], 'foto.jpg', { type: 'image/jpeg' });
            this.cerrarCamara();
            this.cdr.markForCheck();
        }, 'image/jpeg');
    }

    cerrarCamara() {
        if (!this.videoCamara) return;
        this.chatsApi.cerrarCamara(this.videoCamara);
        this.videoCamara = undefined;
    }

    ngOnDestroy() {
        this.cerrarCamara();
    }

    adjuntar(input: HTMLInputElement) {
        this.adjunto = input.files?.[0] ?? this.adjunto;
        input.value = '';
    }

    descargar(archivo: File) {
        this.chatsApi.descargar({ archivo, nombre: archivo.name });
    }

    async enviar() {
        const texto = this.mensajeNuevo.trim();
        const archivo = this.adjunto;
        const contacto = this.seleccionado;
        if (!contacto || this.enviando || (!texto && !archivo)) return;

        this.enviando = true;
        this.avisoMensaje = '';
        const mensaje = { texto, enviado: true, archivo, fallo: false };
        this.mensajes.push(mensaje);
        this.mensajeNuevo = '';
        this.adjunto = undefined;
        try {
            if (texto) {
                if (this.idChat === undefined) {
                    await this.cargarMensajes(contacto, false);
                    if (this.idChat === undefined) {
                        await firstValueFrom(this.chatsApi.crearChat(contacto.id));
                        await this.cargarMensajes(contacto, false);
                    }
                }
                if (this.idChat === undefined) throw new Error('Chat no disponible');
                await firstValueFrom(this.chatsApi.enviarMensaje(this.idChat, texto));
            }
            if (archivo) this.avisoMensaje = 'El adjunto solo est\u00e1 disponible en este navegador.';
        } catch {
            mensaje.fallo = true;
            this.avisoMensaje = 'No se pudo enviar el mensaje.';
        } finally {
            this.enviando = false;
            this.cdr.markForCheck();
        }
    }

    /** Busca GIF con el texto ingresado y actualiza los resultados del chat. */
    buscarGifs(texto: string): void {
        if (!this.auth.estaLogueado()) return;
        const query = texto.trim();
        if (!query || this.buscandoGif) return;
        this.buscandoGif = true;
        this.gifs = [];
        this.avisoGif = '';
        this.gifsApi.buscar(query).subscribe({
            next: lista => {
                this.gifs = lista;
                this.avisoGif = lista.length ? '' : 'No se encontraron GIFs.';
                this.buscandoGif = false;
                this.cdr.markForCheck();
            },
            error: fallo => {
                if (fallo.status === 401) this.auth.avisarVencimiento();
                else this.avisoGif = typeof fallo.error === 'string' ? fallo.error : 'No se pudieron cargar los GIFs.';
                this.buscandoGif = false;
                this.cdr.markForCheck();
            }
        });
    }

    /**
     * Cambia la sección que se muestra en la página principal.
     */
    irASeccion(
        seccion: 'chats' | 'ia' | 'contactos'
    ): void {
        this.seccion = seccion;
        this.mensajeContacto = '';
        this.errorContacto = '';
    }

    /**
     * Cierra la sesión y redirige al usuario al inicio de sesión.
     */
    salir(): void {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }

    /**
     * Abre la pantalla de contactos.
     */
    irAContactos(): void {
        this.router.navigate(['/contactos']);
    }
}