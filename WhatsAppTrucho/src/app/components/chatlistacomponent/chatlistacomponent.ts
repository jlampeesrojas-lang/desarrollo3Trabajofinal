import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Chat } from '../../models/chat';

/** Lista de chats con buscador por nombre o código. */
@Component({
    selector: 'app-chatlistacomponent',
    standalone: false,
    templateUrl: './chatlistacomponent.html',
    styleUrl: './chatlistacomponent.css',
})
export class Chatlistacomponent {
    @Input() chats: Chat[] = [];
    @Input() activoId: number | null = null;
    @Input() miId = 1;
    @Output() readonly seleccionar = new EventEmitter<number>();

    titulochats = 'Chats';
    busqueda = '';
    textoSinMensajes = 'Sin mensajes todavía';
    formatoHora: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit' };
    fotosRotas: number[] = [];

    /** Chats que coinciden con el texto de búsqueda. */
    get filtrados(): Chat[] {
        const texto = this.busqueda.trim().toLowerCase();
        if (!texto) {
            return this.chats;
        }
        return this.chats.filter(c => this.nombre(c).toLowerCase().includes(texto) || c.codigo.toLowerCase().includes(texto));
    }

    /** Nombre del chat: los participantes distintos del usuario. */
    nombre(chat: Chat): string {
        const otros = chat.participantes.filter(p => p.id !== this.miId).map(p => p.nombre);
        return otros.length ? otros.join(', ') : 'Chat sin participantes';
    }

    /** URL de la foto del chat, o undefined si no tiene o no cargó. */
    foto(chat: Chat): string | undefined {
        if (this.fotosRotas.includes(chat.id)) {
            return undefined;
        }
        return chat.participantes.find(p => p.id !== this.miId)?.avatarUrl;
    }

    /** Marca la foto del chat como rota para mostrar las iniciales. */
    fotoRota(chat: Chat): void {
        this.fotosRotas = [...this.fotosRotas, chat.id];
    }

    /** Iniciales del nombre, para mostrar cuando no hay foto. */
    iniciales(chat: Chat): string {
        return this.nombre(chat).split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
    }

    /** Texto de vista previa del último mensaje. */
    previa(chat: Chat): string {
        return chat.ultimoMensaje ? chat.ultimoMensaje.contenido : this.textoSinMensajes;
    }

    /** Hora del último mensaje en formato local (es-CO). */
    hora(chat: Chat): string {
        if (!chat.ultimoMensaje) {
            return '';
        }
        return new Date(chat.ultimoMensaje.fechaEnvio).toLocaleTimeString('es-CO', this.formatoHora);
    }
}
