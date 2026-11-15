import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Chat } from '../../models/chat';

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
    @Output() seleccionar = new EventEmitter<number>();

    titulochats = 'Chats';
    busqueda = '';
    fotosRotas: number[] = [];

    get filtrados(): Chat[] {
        const texto = this.busqueda.trim().toLowerCase();
        if (!texto) {
            return this.chats;
        }
        return this.chats.filter(c => this.nombre(c).toLowerCase().includes(texto) || c.codigo.toLowerCase().includes(texto));
    }

    nombre(chat: Chat): string {
        const otros = chat.participantes.filter(p => p.id !== this.miId).map(p => p.nombre);
        return otros.length ? otros.join(', ') : 'Chat sin participantes';
    }

    foto(chat: Chat): string | undefined {
        if (this.fotosRotas.includes(chat.id)) {
            return undefined;
        }
        return chat.participantes.find(p => p.id !== this.miId)?.avatarUrl;
    }

    fotoRota(chat: Chat): void {
        this.fotosRotas = [...this.fotosRotas, chat.id];
    }

    iniciales(chat: Chat): string {
        return this.nombre(chat).split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
    }

    previa(chat: Chat): string {
        return chat.ultimoMensaje ? chat.ultimoMensaje.contenido : 'Sin mensajes todavía';
    }

    hora(chat: Chat): string {
        if (!chat.ultimoMensaje) {
            return '';
        }
        return new Date(chat.ultimoMensaje.fechaEnvio).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    }
}
