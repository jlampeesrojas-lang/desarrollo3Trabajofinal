import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Mensaje } from '../../models/chat';

@Component({
    selector: 'app-burbujamensajecomponent',
    standalone: false,
    templateUrl: './burbujamensajecomponent.html',
    styleUrl: './burbujamensajecomponent.css',
})
export class Burbujamensajecomponent {
    @Input() mensaje!: Mensaje;
    @Input() esMio = false;
    @Output() reintentar = new EventEmitter<Mensaje>();

    get hora(): string {
        return new Date(this.mensaje.fechaEnvio).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    }
}
