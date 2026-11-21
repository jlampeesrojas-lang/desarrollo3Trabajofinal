import {Subscription} from 'rxjs';
import type {Contacto} from '../../models/contacto';
import type {MensajeHistorial} from '../../models/chat';
import {AuthService} from '../../services/authservice';
import {Chatsservice} from '../../services/chatsservice';
import type {Gif} from '../../models/gif';
import {GifService} from '../../services/gifservice';
import {
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    OnDestroy
} from '@angular/core';
import {Iaservice} from "../../services/iaservice";

/** Muestra la conversación del contacto seleccionado y permite enviar mensajes y adjuntos. */
@Component({
    selector: 'app-chatscomponent',
    standalone: false,
    templateUrl: './chatscomponent.html',
    styleUrl: './chatscomponent.css'
})
export class Chatscomponent implements OnChanges, OnDestroy {

    @Input() contacto: Contacto | null = null;
    chatId: number | null = null;
    mensajes: MensajeHistorial[] = [];
    texto = '';
    archivo: File | null = null;
    enviando = false;
    panelGifAbierto = false;
    buscandoGif = false;
    resultadosGif: Gif[] = [];
    avisoGif = '';
    cargado = false;
    aviso = '';
    avisoEnvio = '';
    camaraAbierta = false;
    usuario: string;
    imagenes = new Map<number, string>();
    menuAbierto = false;
    vistaAudio = '';
    @Input() modoIa = false;

    private solicitudes = new Subscription();
    private cargandoImagenes = new Set<number>();
    private temporizador?: ReturnType<typeof setTimeout>;
    private flujo: MediaStream | null = null;
    private turnoCamara = 0;
    private siguienteIdTemporal = -1;
    private versionEnvio = 0;
    private textoEnProceso = false;

    constructor(
        private servicio: Chatsservice,
        private auth: AuthService,
        private cdr: ChangeDetectorRef,
        private gifsApi: GifService,
        private iaApi: Iaservice
    ) {
        this.usuario = this.auth.obtenerUsuario() || '';
    }

    /** Abre el chat correspondiente al contacto elegido en la página principal. */
    ngOnChanges(): void {
        this.limpiar();
        this.panelGifAbierto = false;
        this.buscandoGif = false;
        this.resultadosGif = [];
        this.avisoGif = '';
        this.chatId = null;
        this.mensajes = [];
        this.texto = '';
        this.archivo = null;
        this.cargado = false;
        this.enviando = false;
        this.aviso = '';
        this.avisoEnvio = '';
        this.textoEnProceso = false;
        this.versionEnvio++;
        let apertura;
        if (this.modoIa) {
            apertura = this.iaApi.abrirChat();
        } else if (this.contacto) {
            apertura = this.servicio.abrirChat(this.contacto.id);
        } else {
            return;
        }

        this.solicitudes.add(apertura.subscribe({
            next: id => {
                this.chatId = id;
                this.leerMensajes(id);
            },
            error: error => {
                this.aviso = 'No se pudo abrir la conversación.';
                this.comprobarSesion(error.status);
                this.cdr.markForCheck();
            }
        }));
    }

    /** Consulta otra vez un segundo después de recibir la respuesta anterior. */
    private leerMensajes(id: number): void {
        const versionAlConsultar = this.versionEnvio;
        this.solicitudes.add(this.servicio.obtenerMensajes(id).subscribe({
            next: lista => {
                if (this.textoEnProceso || versionAlConsultar !== this.versionEnvio) {
                    this.programarLectura(id);
                    return;
                }
                const historial = document.getElementById('historial-chat');
                const primeraCarga = !this.cargado;

                let estabaAbajo = false;
                if (historial) {
                    const distancia = historial.scrollHeight
                        - historial.scrollTop
                        - historial.clientHeight;

                    estabaAbajo = distancia <= 80;
                }

                const ultimoAnterior = this.mensajes[this.mensajes.length - 1];
                this.mensajes = lista.slice().reverse();
                const ultimoActual = this.mensajes[this.mensajes.length - 1];

                const llegoMensaje = ultimoActual != null
                    && ultimoActual.id !== ultimoAnterior?.id;

                if (historial && (primeraCarga || (estabaAbajo && llegoMensaje))) {
                    this.cdr.detectChanges();
                    historial.scrollTop = historial.scrollHeight;
                }
                this.cargado = true;
                this.aviso = '';
                this.actualizarImagenes();
                this.cdr.markForCheck();
                this.programarLectura(id);
            },
            error: error => {
                this.aviso = 'No se pudo actualizar la conversación.';
                this.comprobarSesion(error.status);

                if (![401, 403, 404].includes(error.status)) {
                    this.programarLectura(id);
                }

                this.cdr.markForCheck();
            }
        }));
    }

    /** Programa otra lectura para recibir mensajes de otros usuarios. */
    private programarLectura(id: number): void {
        if (this.modoIa) {
            return;
        }

        this.temporizador = setTimeout(() => this.leerMensajes(id), 500);
    }

    /** Carga cada imagen con autorización y reutiliza su URL en los refrescos. */
    private actualizarImagenes(): void {
        const visibles = new Set(this.mensajes.map(m => m.id));

        for (const [id, url] of this.imagenes) {
            if (!visibles.has(id)) {
                URL.revokeObjectURL(url);
                this.imagenes.delete(id);
            }
        }

        for (const m of this.mensajes) {
            if (
                (m.tipo !== 'IMAGEN' && m.tipo !== 'AUDIO') ||
                !m.mediaUrl ||
                this.imagenes.has(m.id) ||
                this.cargandoImagenes.has(m.id)
            ) {
                continue;
            }

            this.cargandoImagenes.add(m.id);

            this.solicitudes.add(this.servicio.obtenerArchivo(m.mediaUrl).subscribe({
                next: blob => {
                    this.cargandoImagenes.delete(m.id);
                    if (this.mensajes.some(actual => actual.id === m.id)) {
                        this.imagenes.set(m.id, URL.createObjectURL(blob));
                    }
                    this.cdr.markForCheck();
                },
                error: error => {
                    this.cargandoImagenes.delete(m.id);
                    this.comprobarSesion(error.status);
                }
            }));
        }
    }

    /** Selecciona una imagen o documento y permite elegir el mismo archivo otra vez. */
    seleccionarArchivo(input: HTMLInputElement): void {
        const elegido = input.files?.item(0);
        input.value = '';
        if (elegido) this.prepararAdjunto(elegido);
    }

    /** Envía el archivo seleccionado con su texto en un solo mensaje, o solo el texto escrito. */
    enviar(historial: HTMLDivElement): void {
        if (this.chatId === null || this.enviando) return;

        const contenido = this.texto.trim();
        const adjunto = this.archivo;
        if (!adjunto && !contenido) return;

        if (!adjunto) {
            this.enviarTexto(this.chatId, contenido, historial);
            return;
        }

        const chatId = this.chatId;
        const peticion = this.servicio.enviarArchivo(chatId, adjunto, contenido);

        this.enviando = true;
        this.avisoEnvio = '';

        this.solicitudes.add(peticion.subscribe({
            next: () => {
                this.quitarAdjunto();
                this.texto = '';
                this.enviando = false;

                if (this.modoIa) {
                    this.pedirRespuestaIa(chatId);
                }
                this.cdr.markForCheck();
            },
            error: error => {
                this.enviando = false;

                if (error.status === 413) {
                    this.avisoEnvio = 'El servidor rechazó el tamaño del archivo.';
                } else if (error.status === 0) {
                    this.avisoEnvio = 'Se perdió la conexión. Revisa el historial antes de reenviar.';
                } else {
                    let detalle = '';

                    if (typeof error.error === 'string') {
                        try {
                            const cuerpo = JSON.parse(error.error);
                            detalle = cuerpo.message || cuerpo.mensaje || cuerpo.error || '';
                        } catch {
                            if (!error.error.trim().startsWith('<')) {
                                detalle = error.error.trim().slice(0, 180);
                            }
                        }
                    }

                    this.avisoEnvio = `Error ${error.status} al enviar. ${detalle}`.trim();
                }

                this.comprobarSesion(error.status);
                this.cdr.markForCheck();
            }
        }));
    }

    /** El HTML entrega directamente el elemento de video. */
    abrirCamara(video: HTMLVideoElement): void {
        if (this.camaraAbierta) return;

        if (!navigator.mediaDevices?.getUserMedia) {
            this.avisoEnvio = 'La cámara necesita localhost o HTTPS.';
            return;
        }

        this.camaraAbierta = true;
        this.avisoEnvio = '';
        const turno = ++this.turnoCamara;

        navigator.mediaDevices.getUserMedia({
            video: {facingMode: 'environment'},
            audio: false
        }).then(flujo => {
            if (turno !== this.turnoCamara) {
                for (const pista of flujo.getTracks()) {
                    pista.stop();
                }
                return Promise.resolve();
            }

            this.flujo = flujo;
            video.srcObject = flujo;
            return video.play();
        }).catch(() => {
            if (turno !== this.turnoCamara) return;

            this.cerrarCamara();
            this.avisoEnvio = 'No se pudo abrir la cámara. Revisa el permiso del navegador.';
            this.cdr.markForCheck();
        });
    }

    /** Deja la foto lista para enviar como imagen JPEG. */
    tomarFoto(video: HTMLVideoElement): void {
        if (!video.videoWidth || !video.videoHeight) return;

        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        const contexto = canvas.getContext('2d');
        if (!contexto) return;

        contexto.drawImage(video, 0, 0);
        const turno = this.turnoCamara;

        canvas.toBlob(blob => {
            if (turno !== this.turnoCamara) return;

            if (blob) {
                this.prepararAdjunto(new File([blob], 'foto.jpg', {type: 'image/jpeg'}));
            } else {
                this.avisoEnvio = 'No se pudo guardar la foto.';
            }

            this.cerrarCamara();
            this.cdr.markForCheck();
        }, 'image/jpeg', 0.85);
    }

    /** Detiene las pistas de video y cierra la vista previa de la cámara. */
    cerrarCamara(): void {
        this.turnoCamara++;
        this.flujo?.getTracks().forEach(pista => pista.stop());
        this.flujo = null;
        this.camaraAbierta = false;
    }

    /** Al pulsar el nombre se descarga el documento. */
    descargar(mensaje: MensajeHistorial): void {
        if (!mensaje.mediaUrl) return;

        this.solicitudes.add(this.servicio.obtenerArchivo(mensaje.mediaUrl).subscribe({
            next: blob => {
                const url = URL.createObjectURL(blob);
                const enlace = document.createElement('a');
                enlace.href = url;
                enlace.download = mensaje.contenido;
                enlace.click();
                setTimeout(() => URL.revokeObjectURL(url), 1000);
            },
            error: error => {
                this.avisoEnvio = 'No se pudo descargar el archivo.';
                this.comprobarSesion(error.status);
                this.cdr.markForCheck();
            }
        }));
    }

    /** Notifica el vencimiento de la sesión cuando el servidor responde con HTTP 401. */
    private comprobarSesion(estado: number): void {
        if (estado === 401) this.auth.avisarVencimiento();
    }

    /** Cancela las consultas pendientes y libera los recursos de la conversación. */
    private limpiar(): void {
        this.menuAbierto = false;
        this.quitarAdjunto();
        clearTimeout(this.temporizador);
        this.solicitudes.unsubscribe();
        this.solicitudes = new Subscription();
        this.cerrarCamara();

        for (const url of this.imagenes.values()) {
            URL.revokeObjectURL(url);
        }

        this.imagenes.clear();
        this.cargandoImagenes.clear();
    }

    /** Libera los recursos del chat cuando Angular destruye el componente. */
    ngOnDestroy(): void {
        this.limpiar();
    }

    /** Busca GIF para la conversación abierta. */
    buscarGif(termino: string): void {
        const consulta = termino.trim();

        if (!consulta || this.buscandoGif || this.chatId === null) {
            return;
        }

        this.buscandoGif = true;
        this.avisoGif = '';
        this.resultadosGif = [];

        this.solicitudes.add(
            this.gifsApi.buscar(consulta).subscribe({
                next: encontrados => {
                    this.resultadosGif = encontrados;
                    this.buscandoGif = false;

                    if (encontrados.length === 0) {
                        this.avisoGif = 'No se encontraron GIFs.';
                    }

                    this.cdr.markForCheck();
                },
                error: error => {
                    this.buscandoGif = false;
                    this.avisoGif = 'No se pudieron buscar los GIFs.';
                    this.comprobarSesion(error.status);
                    this.cdr.markForCheck();
                }
            })
        );
    }

    /** Envía el GIF seleccionado y cierra el buscador al recibir confirmación. */
    enviarGif(gif: Gif): void {
        if (this.chatId === null || this.enviando) {
            return;
        }

        this.enviando = true;
        this.avisoGif = '';

        const chatId = this.chatId;

        this.solicitudes.add(
            this.servicio.enviarGif(chatId, gif.originalUrl).subscribe({
                next: () => {
                    this.enviando = false;
                    this.panelGifAbierto = false;
                    this.resultadosGif = [];

                    if (this.modoIa) {
                        this.pedirRespuestaIa(chatId);
                    }
                    this.cdr.markForCheck();
                },
                error: error => {
                    this.enviando = false;
                    this.avisoGif = 'No se pudo confirmar el envío del GIF.';
                    this.comprobarSesion(error.status);
                    this.cdr.markForCheck();
                }
            })
        );
    }

    /**
     * Muestra el adjunto recién enviado y pide a la IA que responda al último mensaje del chat.
     * Mientras la IA responde se muestra el aviso de que está escribiendo.
     */
    private pedirRespuestaIa(chatId: number, pregunta = ''): void {
        this.enviando = true;
        this.avisoEnvio = '';
        this.leerMensajes(chatId);

        this.solicitudes.add(this.iaApi.responder(chatId, pregunta).subscribe({
            next: () => {
                this.enviando = false;
                this.leerMensajes(chatId);
                this.cdr.markForCheck();
            },
            error: error => {
                this.enviando = false;

                if (pregunta) {
                    this.texto = pregunta;
                }

                if (error.status === 502) {
                    this.avisoEnvio = 'La IA no pudo revisar el adjunto. Intenta de nuevo en un momento.';
                } else if (error.status === 0) {
                    this.avisoEnvio = 'Se perdió la conexión. Revisa el historial antes de reenviar.';
                } else {
                    this.avisoEnvio = `La IA no pudo responder (HTTP ${error.status}).`;
                }

                this.comprobarSesion(error.status);
                this.leerMensajes(chatId);
                this.cdr.markForCheck();
            }
        }));
    }

    /** Valida el archivo seleccionado y prepara la vista previa si es un audio. */
    prepararAdjunto(elegido: File): void {
        if (!elegido.size || elegido.size > 10 * 1024 * 1024) {
            this.avisoEnvio = 'El archivo debe tener contenido y pesar máximo 10 MB.';
            return;
        }

        this.quitarAdjunto();
        this.archivo = elegido;
        this.avisoEnvio = '';

        if (elegido.type.startsWith('audio/')) {
            this.vistaAudio = URL.createObjectURL(elegido);
        }

        this.cdr.markForCheck();
    }

    /** Descarta el archivo pendiente y libera la URL de la vista previa de audio. */
    quitarAdjunto(): void {
        if (this.vistaAudio) {
            URL.revokeObjectURL(this.vistaAudio);
        }
        this.vistaAudio = '';
        this.archivo = null;
    }

    /** Muestra el texto de inmediato y actualiza la burbuja cuando el servidor confirma el envío. */
    private enviarTexto(chatId: number, contenido: string, historial: HTMLDivElement): void {
        const idTemporal = this.siguienteIdTemporal--;

        const pendiente: MensajeHistorial = {
            id: idTemporal,
            remitenteNombre: this.usuario,
            contenido,
            tipo: 'TEXTO',
            mediaUrl: null,
            fechaEnvio: new Date().toISOString()
        };

        this.versionEnvio++;
        this.textoEnProceso = true;
        this.enviando = true;
        this.avisoEnvio = '';
        this.texto = '';

        this.mensajes.push(pendiente);
        this.cdr.detectChanges();
        historial.scrollTop = historial.scrollHeight;
        if (this.modoIa) {
            this.solicitudes.add(this.iaApi.preguntar(chatId, contenido).subscribe({
                next: () => {
                    this.versionEnvio++;
                    this.textoEnProceso = false;
                    this.enviando = false;

                    // Spring ya guardó la pregunta y la respuesta con sus ids reales.
                    this.leerMensajes(chatId);
                    this.cdr.markForCheck();
                },
                error: error => {
                    this.versionEnvio++;
                    this.textoEnProceso = false;
                    this.enviando = false;
                    this.mensajes = this.mensajes.filter(m => m.id !== idTemporal);
                    this.texto = contenido;

                    if (error.status === 502) {
                        this.avisoEnvio = 'La IA no pudo responder. Intenta de nuevo en un momento.';
                    } else if (error.status === 0) {
                        this.avisoEnvio = 'Se perdió la conexión. Revisa el historial antes de reenviar.';
                    } else {
                        this.avisoEnvio = `No se pudo enviar la pregunta (HTTP ${error.status}).`;
                    }

                    this.comprobarSesion(error.status);
                    this.cdr.markForCheck();
                }
            }));

            return;
        }

        this.solicitudes.add(
            this.servicio.enviarMensaje(chatId, contenido).subscribe({
                next: guardado => {
                    this.versionEnvio++;
                    this.textoEnProceso = false;
                    this.enviando = false;

                    this.mensajes = this.mensajes.map(m =>
                        m.id === idTemporal ? guardado : m
                    );

                    this.cdr.markForCheck();
                },
                error: error => {
                    this.versionEnvio++;
                    this.textoEnProceso = false;
                    this.enviando = false;

                    this.mensajes = this.mensajes.filter(m => m.id !== idTemporal);
                    this.texto = contenido;

                    this.avisoEnvio = error.status === 0
                        ? 'Se perdió la conexión. Revisa el historial antes de reenviar.'
                        : `No se pudo enviar el mensaje (HTTP ${error.status}).`;

                    this.comprobarSesion(error.status);
                    this.cdr.markForCheck();
                }
            })
        );
    }
}