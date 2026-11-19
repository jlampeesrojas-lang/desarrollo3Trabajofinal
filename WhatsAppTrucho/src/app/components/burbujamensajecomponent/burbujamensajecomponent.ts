import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Mensaje } from '../../models/chat';

/** Burbuja de un mensaje: muestra el texto, la hora, el autor y el estado de envío. */
@Component({
    selector: 'app-burbujamensajecomponent',
    standalone: false,
    templateUrl: './burbujamensajecomponent.html',
    styleUrl: './burbujamensajecomponent.css',
})
export class Burbujamensajecomponent {
    @Input() mensaje!: Mensaje;
    @Input() esMio = false;
    @Output() readonly reintentar = new EventEmitter<Mensaje>();

    /** Hora del mensaje en formato local (es-CO). */
    get hora(): string {
        return new Date(this.mensaje.fechaEnvio).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    }
}
