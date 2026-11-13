import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/** Avisa a la lista que algo cambió (mensaje enviado, chat leído). */
@Injectable()
export class ChatEventosService {
  private readonly _cambios = new Subject<void>();
  readonly cambios$ = this._cambios.asObservable();

  notificarCambio(): void {
    this._cambios.next();
  }
}
