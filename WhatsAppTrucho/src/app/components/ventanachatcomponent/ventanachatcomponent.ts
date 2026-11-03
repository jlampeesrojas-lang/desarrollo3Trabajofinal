import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, catchError, forkJoin, map, switchMap, tap } from 'rxjs';
import { Chat, avatarDelChat, nombreDelChat } from '../../models/chatmodel';
import { Mensaje } from '../../models/mensajemodel';
import { CHAT_USUARIO_ID, ChatApi } from '../../services/chatapi';
import { ChatEventosService } from '../../services/chateventosservice';
import { Burbujamensajecomponent } from '../burbujamensajecomponent/burbujamensajecomponent';

const TAMANO_PAGINA = 20;

@Component({
  selector: 'app-ventanachatcomponent',
  standalone: true,
  imports: [FormsModule, RouterLink, Burbujamensajecomponent],
  templateUrl: './ventanachatcomponent.html',
  styleUrl: './ventanachatcomponent.css',
})
export class Ventanachatcomponent {
  private readonly api = inject(ChatApi);
  private readonly eventos = inject(ChatEventosService);
  private readonly route = inject(ActivatedRoute);
  readonly miId = inject(CHAT_USUARIO_ID);

  @ViewChild('scroller') scroller?: ElementRef<HTMLElement>;

  readonly chat = signal<Chat | null>(null);
  readonly mensajes = signal<Mensaje[]>([]);
  readonly cargando = signal(true);
  readonly noExiste = signal(false);
  readonly errorCarga = signal(false);
  readonly cargandoAnteriores = signal(false);
  readonly hayAnteriores = signal(false);
  texto = '';

  private pagina = 0;
  private totalPaginas = 1;
  private contadorTmp = 0;

  constructor() {
    this.route.paramMap.pipe(
      tap(() => {
        this.cargando.set(true); this.noExiste.set(false); this.errorCarga.set(false);
        this.chat.set(null); this.mensajes.set([]); this.texto = '';
      }),
      map(p => Number(p.get('id'))),
      switchMap(id => forkJoin({ chat: this.api.obtenerChat(id), pag: this.api.listarMensajes(id, 0, TAMANO_PAGINA) }).pipe(
        catchError(err => {
          if (err?.status === 404) this.noExiste.set(true); else this.errorCarga.set(true);
          this.cargando.set(false);
          return EMPTY;
        }),
      )),
      takeUntilDestroyed(),
    ).subscribe(({ chat, pag }) => {
      this.chat.set(chat);
      this.mensajes.set(pag.items);
      this.pagina = 0;
      this.totalPaginas = pag.totalPages;
      this.hayAnteriores.set(this.totalPaginas > 1);
      this.cargando.set(false);
      this.irAbajo();
      this.api.marcarLeido(chat.id).subscribe(() => this.eventos.notificarCambio());
    });
  }

  avatar(c: Chat): string | undefined { return avatarDelChat(c, this.miId); }
  nombre(c: Chat): string { return nombreDelChat(c, this.miId); }
  iniciales(c: Chat): string {
    return this.nombre(c).split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
  }

  cargarAnteriores(): void {
    const chat = this.chat();
    if (!chat || this.cargandoAnteriores()) return;
    const el = this.scroller?.nativeElement;
    const alturaPrevia = el?.scrollHeight ?? 0;
    const topPrevio = el?.scrollTop ?? 0;
    this.cargandoAnteriores.set(true);
    this.api.listarMensajes(chat.id, this.pagina + 1, TAMANO_PAGINA).subscribe({
      next: pag => {
        this.pagina = pag.page;
        this.mensajes.update(m => [...pag.items, ...m]);
        this.hayAnteriores.set(this.pagina + 1 < pag.totalPages);
        this.cargandoAnteriores.set(false);
        // mantiene la posición: el usuario sigue viendo lo mismo que antes
        setTimeout(() => { if (el) el.scrollTop = el.scrollHeight - alturaPrevia + topPrevio; });
      },
      error: () => this.cargandoAnteriores.set(false),
    });
  }

  enter(ev: Event): void {
    if ((ev as KeyboardEvent).shiftKey) return;
    ev.preventDefault();
    this.enviar();
  }

  enviar(): void {
    const chat = this.chat();
    const contenido = this.texto.trim();
    if (!chat || !contenido) return;
    this.texto = '';
    const tmp: Mensaje = {
      id: `tmp-${++this.contadorTmp}`, chatId: chat.id, remitenteId: this.miId, remitenteNombre: 'Tú',
      contenido, tipo: 'TEXTO', fechaEnvio: new Date().toISOString(), estado: 'ENVIANDO',
    };
    this.mensajes.update(m => [...m, tmp]);
    this.irAbajo();
    this.transmitir(tmp);
  }

  reintentar(m: Mensaje): void {
    this.reemplazar(m.id, { ...m, estado: 'ENVIANDO' });
    this.transmitir({ ...m, estado: 'ENVIANDO' });
  }

  private transmitir(tmp: Mensaje): void {
    this.api.enviarMensaje(tmp.chatId, tmp.contenido).subscribe({
      next: real => { this.reemplazar(tmp.id, real); this.eventos.notificarCambio(); },
      error: () => this.reemplazar(tmp.id, { ...tmp, estado: 'ERROR' }),
    });
  }

  private reemplazar(id: number | string, nuevo: Mensaje): void {
    this.mensajes.update(lista => lista.map(x => (x.id === id ? nuevo : x)));
  }

  private irAbajo(): void {
    setTimeout(() => {
      const el = this.scroller?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
