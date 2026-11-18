import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {HttpErrorResponse} from "@angular/common/http";
import {AuthService} from "../../services/authservice";
import {AdminService} from "../../services/adminservice";
import {ChatAdmin, ClaveChat, MensajeAdmin} from "../../models/admin";

@Component({
    selector: "app-adminchatscomponent",
    standalone: false,
    styleUrl: "./adminchatscomponent.css",
    templateUrl: "./adminchatscomponent.html",
})
/** Visor de chats del administrador: lista los chats y muestra sus mensajes al ingresar la clave. */
export class Adminchatscomponent implements OnInit {
    chats: ChatAdmin[] = [];
    cargandoChats = false;
    errorChats = "";
    busquedaChat = "";
    chatSeleccionado: ChatAdmin | null = null;
    mensajes: MensajeAdmin[] = [];
    cargandoMensajes = false;
    claveChat = "";
    claveVisible = false;
    desbloqueado = false;
    errorClave = "";
    textosMedia: { [tipo: string]: string } = {IMAGEN: "[Imagen]", AUDIO: "[Audio]"};
    separadorParticipantes = " y ";

    constructor(private auth: AuthService, private admin: AdminService,
                private router: Router, private cdr: ChangeDetectorRef) {
    }

    /** Al abrir la sección se cargan los chats. */
    ngOnInit() {
        this.cargarChats();
    }

    /** Convierte un error HTTP en un texto para el usuario. 401 cierra la sesión y 403 avisa que no hay permiso. */
    mensajeDeError(fallo: HttpErrorResponse, mensajePorDefecto: string): string {
        if (fallo.status === 401) {
            this.auth.cerrarSesion();
            this.router.navigate(["/login"]);
            return "Tu sesión venció, inicia sesión de nuevo";
        }
        if (typeof fallo.error === "string" && fallo.error !== "") {
            return fallo.error;
        }
        if (fallo.error?.mensaje) {
            return fallo.error.mensaje;
        }
        if (fallo.status === 0) {
            return "No se pudo conectar con el servidor";
        }
        if (fallo.status === 403) {
            return "No tienes permiso para hacer esto";
        }
        return mensajePorDefecto;
    }

    /** Pide al back la lista de chats. */
    cargarChats() {
        this.cargandoChats = true;
        this.errorChats = "";

        this.admin.listarChats().subscribe({
            next: (lista: ChatAdmin[]) => {
                if (lista == null) {
                    this.chats = [];
                } else {
                    this.chats = lista;
                }
                this.cargandoChats = false;
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.cargandoChats = false;
                this.errorChats = this.mensajeDeError(fallo, "No se pudieron cargar los chats");
                this.cdr.markForCheck();
            },
        });
    }

    /** Chats que coinciden con lo escrito en el buscador (por código o participante). */
    chatsFiltrados(): ChatAdmin[] {
        const texto = this.busquedaChat.trim().toLowerCase();
        if (texto === "") {
            return this.chats;
        }
        const resultado: ChatAdmin[] = [];
        for (const chat of this.chats) {
            const todo = `${chat.code} ${chat.participants.join(" ")}`.toLowerCase();
            if (todo.indexOf(texto) !== -1) {
                resultado.push(chat);
            }
        }
        return resultado;
    }

    /** Nombres de los participantes de un chat separados por una "y". */
    participantesDe(chat: ChatAdmin): string {
        return chat.participants.join(this.separadorParticipantes);
    }

    /** Selecciona un chat y lo deja bloqueado hasta que se ingrese la clave. */
    abrirChat(chat: ChatAdmin) {
        this.chatSeleccionado = chat;
        this.mensajes = [];
        this.desbloqueado = false;
        this.claveChat = "";
        this.claveVisible = false;
        this.errorClave = "";
    }

    /** Pide al back la clave secreta del chat (solo admin) y la deja escrita en el campo. */
    verClaveSecreta() {
        const chat = this.chatSeleccionado;
        if (chat == null) {
            return;
        }
        this.errorClave = "";
        this.admin.claveDeChat(chat.id).subscribe({
            next: (respuesta: ClaveChat) => {
                this.claveChat = respuesta.key;
                this.claveVisible = true;
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.errorClave = this.mensajeDeError(fallo, "No se pudo obtener la clave");
                this.cdr.markForCheck();
            },
        });
    }

    /** Manda la clave al back. Si es correcta, recibe los mensajes ya descifrados. */
    desbloquearChat() {
        const chat = this.chatSeleccionado;
        if (chat == null) {
            return;
        }
        this.errorClave = "";
        if (this.claveChat.length !== 16) {
            this.errorClave = "La clave del chat tiene 16 caracteres";
            return;
        }

        this.cargandoMensajes = true;
        this.admin.mensajesDeChat(chat.id, this.claveChat).subscribe({
            next: (lista: MensajeAdmin[]) => {
                const enOrden: MensajeAdmin[] = [];
                if (lista != null) {
                    for (let posicion = lista.length - 1; posicion >= 0; posicion--) {
                        enOrden.push(lista[posicion]);
                    }
                }
                this.mensajes = enOrden;
                this.desbloqueado = true;
                this.cargandoMensajes = false;
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.cargandoMensajes = false;
                this.desbloqueado = false;
                this.errorClave = this.mensajeDeError(fallo, "Clave incorrecta");
                this.cdr.markForCheck();
            },
        });
    }

    /** Vuelve a ocultar los mensajes y borra la clave escrita. */
    bloquearChat() {
        this.mensajes = [];
        this.desbloqueado = false;
        this.claveChat = "";
        this.claveVisible = false;
    }

    /** Texto de un mensaje. Las imágenes y los audios se muestran con una etiqueta. */
    contenidoMensaje(mensaje: MensajeAdmin): string {
        const etiqueta = this.textosMedia[mensaje.type];
        if (etiqueta) {
            return etiqueta;
        }
        return mensaje.content;
    }

    get mostrarAvisoCifrado(): boolean {
        return !this.desbloqueado && !this.cargandoMensajes;
    }

    get mostrarChatVacio(): boolean {
        return this.desbloqueado && this.mensajes.length === 0;
    }

    get tipoCampoClave(): string {
        if (this.claveVisible) {
            return "text";
        }
        return "password";
    }
}