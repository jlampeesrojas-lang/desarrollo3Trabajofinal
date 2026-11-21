import {
    Component,
    OnInit,
    OnDestroy,
    ViewChild,
    ElementRef
} from '@angular/core';

import { AuthService } from '../../services/authservice';
import { Chatsservice } from '../../services/chatsservice';

/**
 * Componente de conversaciones.
 * Muestra la lista de chats, los mensajes del chat seleccionado y permite
 * enviar mensajes de texto, adjuntar archivos y tomar fotos con la cámara.
 */
@Component({
    selector: 'app-chatscomponent',
    standalone: false,
    templateUrl: './chatscomponent.html',
    styleUrl: './chatscomponent.css'
})
export class Chatscomponent implements OnInit, OnDestroy {

    /** Conversaciones disponibles con sus mensajes. */
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

    /** Indica si las conversaciones se están cargando. */
    cargando = true;

    /** Indica si hay un mensaje o una foto enviándose. */
    enviando = false;

    /** Mensaje de error que se muestra al usuario. */
    error = '';

    /** Identificador del chat seleccionado. */
    seleccionado = '1';

    /** Texto para filtrar las conversaciones por nombre. */
    busqueda = '';

    /** Nombre del usuario que inició sesión. */
    usuario = '';

    /** Borradores de cada chat (texto y archivos sin enviar), indexados por id del chat. */
    borradores: any = {};

    /** Indica si la cámara está abierta. */
    camaraAbierta = false;

    /** Indica si la cámara se está abriendo. */
    abriendoCamara = false;

    /** Elemento de video donde se muestra la cámara. */
    @ViewChild('videoCamara')
    videoCamara!: ElementRef<HTMLVideoElement>;

    /**
     * Inicializa los servicios de autenticación y de chats.
     *
     * @param auth     Servicio de autenticación.
     * @param servicio Servicio de chats, cámara y descargas.
     */
    constructor(
        private auth: AuthService,
        private servicio: Chatsservice
    ) {}

    /**
     * Carga el nombre del usuario que inició sesión.
     */
    ngOnInit() {
        this.usuario = this.auth.obtenerUsuario() || 'Usuario';
        this.cargando = false;
    }

    /**
     * Obtiene el chat seleccionado.
     *
     * @returns El chat seleccionado, o null si no existe.
     */
    get chat() {
        for (const chat of this.chats) {
            if (chat.id === this.seleccionado) {
                return chat;
            }
        }
        return null;
    }

    /**
     * Filtra las conversaciones según el texto de búsqueda.
     *
     * @returns Las conversaciones cuyo nombre contiene el texto buscado.
     */
    get filtrados() {
        return this.chats.filter(c =>
            c.nombre.toLowerCase().includes(this.busqueda.toLowerCase())
        );
    }

    /**
     * Cambia la conversación seleccionada.
     *
     * @param id Identificador del chat que se desea abrir.
     */
    seleccionarChat(id: string) {
        this.cerrarCamara();
        this.seleccionado = id;
        this.error = '';
    }

    /**
     * Obtiene el borrador del chat seleccionado. Si no existe, lo crea vacío.
     *
     * @returns El borrador con el texto y los archivos sin enviar.
     */
    get borrador() {
        if (!this.borradores[this.seleccionado]) {
            this.borradores[this.seleccionado] = {
                texto: '',
                archivos: []
            };
        }
        return this.borradores[this.seleccionado];
    }

    /**
     * Obtiene el texto del último mensaje de una conversación.
     *
     * @param conversacion Conversación que se desea consultar.
     * @returns El texto del último mensaje, 'Archivo adjunto' si no tiene texto,
     *          o 'Sin mensajes' si la conversación está vacía.
     */
    ultimoMensaje(conversacion: { mensajes: { texto?: string | null }[] }) {
        if (conversacion.mensajes.length === 0) {
            return 'Sin mensajes';
        }

        const mensajes = conversacion.mensajes;
        const ultimo = mensajes[mensajes.length - 1];

        return ultimo.texto || 'Archivo adjunto';
    }

    /**
     * Envía el mensaje del borrador (texto y archivos) al chat seleccionado
     * y deja el borrador vacío.
     */
    enviar() {
        const conversacion = this.chat;

        if (conversacion === null || this.enviando) {
            return;
        }

        const texto = this.borrador.texto.trim();

        if (texto === '' && this.borrador.archivos.length === 0) {
            return;
        }

        this.enviando = true;

        const adjuntos: { nombre: string; archivo: File }[] = [];

        for (const archivo of this.borrador.archivos) {
            adjuntos.push({
                nombre: archivo.name,
                archivo
            });
        }

        const mensaje = {
            id: Date.now().toString() + Math.random().toString(),
            texto,
            enviado: true,
            fecha: new Date(),
            adjuntos
        };

        conversacion.mensajes.push(mensaje);

        this.borradores[this.seleccionado] = {
            texto: '',
            archivos: []
        };

        this.error = '';
        this.enviando = false;
    }

    /**
     * Agrega al borrador los archivos elegidos por el usuario.
     *
     * @param event Evento del campo de selección de archivos.
     */
    adjuntar(event: Event) {
        const input = event.target as HTMLInputElement;

        if (input.files) {
            for (const file of input.files) {
                this.borrador.archivos.push(file);
            }
        }

        input.value = '';
    }

    /**
     * Quita un archivo del borrador.
     *
     * @param posicion Posición del archivo en la lista de archivos del borrador.
     */
    quitarArchivo(posicion: number) {
        this.borrador.archivos.splice(posicion, 1);
    }

    /**
     * Espera a que la cámara entregue imagen, revisando cada 100 ms
     * hasta un máximo de 5 segundos.
     *
     * @param video Elemento de video de la cámara.
     * @throws Error si la cámara no entrega imagen a tiempo.
     */
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

    /**
     * Abre la cámara y muestra la imagen en el elemento de video.
     */
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
                this.error = `Error de cámara: ${error.name} - ${error.message}`;
            } else {
                this.error = 'No se pudo abrir la cámara';
            }
        } finally {
            this.abriendoCamara = false;
        }
    }

    /**
     * Toma una foto con la cámara y la envía como adjunto al chat seleccionado.
     */
    async tomarFoto() {
        if (this.enviando) {
            return;
        }

        const conversacion = this.chat;

        if (!conversacion) {
            this.error = 'No hay una conversación seleccionada';
            return;
        }

        const video = this.videoCamara.nativeElement;

        if (video.videoWidth === 0 || video.videoHeight === 0) {
            this.error = 'La cámara todavía no está lista';
            return;
        }

        this.enviando = true;

        try {
            const canvas = document.createElement('canvas');

            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;

            const contexto = canvas.getContext('2d');

            if (!contexto) {
                throw new Error('No se pudo capturar la imagen');
            }

            contexto.drawImage(video, 0, 0);

            const blob = await new Promise<Blob | null>(resolve => {
                canvas.toBlob(resolve, 'image/jpeg', 0.85);
            });

            if (!blob) {
                throw new Error('No se pudo crear la foto');
            }

            const foto = new File(
                [blob],
                `foto-${Date.now()}.jpg`,
                { type: 'image/jpeg' }
            );

            conversacion.mensajes.push({
                id: `${Date.now().toString()}${Math.random()}`,
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
        } catch (error) {
            console.error('Error al tomar foto:', error);

            this.error = 'No se pudo tomar la fotografía';
        } finally {
            this.enviando = false;
        }
    }

    /**
     * Cierra la cámara y libera el dispositivo.
     */
    cerrarCamara() {
        this.servicio.cerrarCamara(
            this.videoCamara?.nativeElement
        );

        this.camaraAbierta = false;
    }

    /**
     * Descarga un archivo adjunto de un mensaje.
     *
     * @param adjunto Archivo adjunto con su nombre y contenido.
     */
    descargar(adjunto: any) {
        this.servicio.descargar(adjunto);
    }

    /**
     * Cierra la cámara al salir del componente.
     */
    ngOnDestroy() {
        this.cerrarCamara();
    }
}