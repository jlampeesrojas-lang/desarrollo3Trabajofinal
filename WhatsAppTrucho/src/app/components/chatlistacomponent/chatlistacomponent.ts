import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Chat, Participante, avatarDelChat, nombreDelChat } from '../../modelsChat/chatmodel';
import { CHAT_USUARIO_ID, ChatApi } from '../../servicesChat/chatapi';
import { ChatEventosService } from '../../servicesChat/chateventosservice';

@Component({
  selector: 'app-chatlistacomponent',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './chatlistacomponent.html',
  styleUrl: './chatlistacomponent.css',
})
export class Chatlistacomponent {
  private readonly api = inject(ChatApi);
  private readonly miId = inject(CHAT_USUARIO_ID);
  private readonly router = inject(Router);
  private readonly eventos = inject(ChatEventosService);

  readonly chats = signal<Chat[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly busqueda = signal('');

  // ---- crear chat ----
  readonly creando = signal(false); // panel "Nuevo chat" abierto
  readonly contactos = signal<Participante[]>([]);
  readonly cargandoContactos = signal(false);
  readonly errorContactos = signal(false);
  readonly errorCrear = signal(false);
  readonly creandoChat = signal(false); // petición en curso

  readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const ordenados = [...this.chats()].sort((a, b) =>
      (b.ultimoMensaje?.fechaEnvio ?? b.fechaCreacion).localeCompare(a.ultimoMensaje?.fechaEnvio ?? a.fechaCreacion));
    return q ? ordenados.filter(c => this.nombre(c).toLowerCase().includes(q) || c.codigo.toLowerCase().includes(q)) : ordenados;
  });

  constructor() {
    this.cargar();
    inject(ChatEventosService).cambios$.pipe(takeUntilDestroyed()).subscribe(() => this.refrescar());
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api.listarChats().subscribe({
      next: c => { this.chats.set(c); this.cargando.set(false); },
      error: () => { this.error.set(true); this.cargando.set(false); },
    });
  }

  private refrescar(): void {
    this.api.listarChats().subscribe({ next: c => this.chats.set(c) });
  }

  abrirNuevoChat(): void {
    this.creando.set(true);
    this.errorCrear.set(false);
    this.errorContactos.set(false);
    this.cargandoContactos.set(true);
    this.api.listarContactos().subscribe({
      next: c => { this.contactos.set(c); this.cargandoContactos.set(false); },
      error: () => { this.errorContactos.set(true); this.cargandoContactos.set(false); },
    });
  }

  cerrarNuevoChat(): void {
    this.creando.set(false);
  }

  yaTieneChat(contacto: Participante): boolean {
    return this.chats().some(c => c.participantes.some(p => p.id === contacto.id));
  }

  crearChatCon(contacto: Participante): void {
    if (this.creandoChat()) return;
    this.creandoChat.set(true);
    this.errorCrear.set(false);
    this.api.crearChat(contacto.id).subscribe({
      next: chat => {
        this.creandoChat.set(false);
        this.creando.set(false);
        this.eventos.notificarCambio();
        this.router.navigate(['/chats', chat.id]);
      },
      error: () => { this.creandoChat.set(false); this.errorCrear.set(true); },
    });
  }

  nombre(c: Chat): string { return nombreDelChat(c, this.miId); }

  iniciales(c: Chat): string {
    return this.inicialesDe(this.nombre(c));
  }

  avatar(c: Chat): string | undefined { return avatarDelChat(c, this.miId); }

  inicialesDe(nombre: string): string {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
  }

  previa(c: Chat): string {
    const m = c.ultimoMensaje;
    if (!m) return 'Sin mensajes todavía';
    return (m.remitenteId === this.miId ? 'Tú: ' : '') + m.contenido;
  }

  hora(iso: string): string {
    const f = new Date(iso);
    const hoy = new Date();
    if (f.toDateString() === hoy.toDateString()) {
      return f.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
    }
    return f.toLocaleDateString('es-CO', { day: '2-digit', month: '2-digit' });
  }
}
