import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Mensaje } from '../../modelsChat/mensajemodel';

@Component({
  selector: 'app-burbujamensajecomponent',
  standalone: true,
  templateUrl: './burbujamensajecomponent.html',
  styleUrl: './burbujamensajecomponent.css',
})
export class Burbujamensajecomponent {
  @Input({ required: true }) mensaje!: Mensaje;
  @Input() esMio = false;
  @Output() reintentar = new EventEmitter<Mensaje>();

  get hora(): string {
    return new Date(this.mensaje.fechaEnvio).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }
}
