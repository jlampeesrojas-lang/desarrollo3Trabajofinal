import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import { Chat, EstadoConexion, Mensaje } from '../../models/chat';

const TAMANO_PAGINA = 20;

/** Ventana de conversación: historial con scroll, caja de texto y estado de conexión. */
@Component({
    selector: 'app-ventanachatcomponent',
    standalone: false,
    templateUrl: './ventanachatcomponent.html',
    styleUrl: './ventanachatcomponent.css',
})
export class Ventanachatcomponent implements OnChanges, OnDestroy {
    @Input() chat!: Chat;
    @Input() mensajes: Mensaje[] = [];
    @Input() miId = 1;
    @Input() conexion: EstadoConexion = 'CONECTADO';
    @Input() conexionRestablecida = false;

    @Output() readonly enviar = new EventEmitter<string>();
    @Output() readonly reintentar = new EventEmitter<Mensaje>();
    @Output() readonly volver = new EventEmitter<void>();
    @Output() readonly simularCaida = new EventEmitter<void>();

    @ViewChild('scroller') scroller?: ElementRef<HTMLElement>;
    @ViewChild('caja') caja?: ElementRef<HTMLTextAreaElement>;

    cantidadVisible = TAMANO_PAGINA;
    cargandoAnteriores = false;
    foto = true;
    texto = '';

    private temporizador: ReturnType<typeof setTimeout> | null = null;

    constructor(private cdr: ChangeDetectorRef) {}

    /** Reinicia el historial al cambiar de chat y baja al fondo con mensajes nuevos. */
    ngOnChanges(cambios: SimpleChanges): void {
        if (cambios['chat'] && !cambios['chat'].firstChange) {
            this.cantidadVisible = TAMANO_PAGINA;
            this.foto = true;
            this.irAbajo();
        }
        const lista = cambios['mensajes'];
        if (lista && this.mensajes.length > (lista.previousValue?.length ?? 0)) {
            this.irAbajo();
        }
    }

    /** Cancela el temporizador pendiente al destruir el componente. */
    ngOnDestroy(): void {
        if (this.temporizador) {
            clearTimeout(this.temporizador);
        }
    }

    /** Mensajes que se muestran ahora (los más recientes). */
    get visibles(): Mensaje[] {
        return this.mensajes.slice(-this.cantidadVisible);
    }

    /** Indica si quedan mensajes anteriores por cargar. */
    get hayAnteriores(): boolean {
        return this.mensajes.length > this.cantidadVisible;
    }

    /** Nombre del chat: los participantes distintos del usuario. */
    get nombre(): string {
        const otros = this.chat.participantes.filter(p => p.id !== this.miId).map(p => p.nombre);
        return otros.length ? otros.join(', ') : 'Chat sin participantes';
    }

    /** URL de la foto del chat, o undefined si no tiene o no cargó. */
    get avatar(): string | undefined {
        return this.foto ? this.chat.participantes.find(p => p.id !== this.miId)?.avatarUrl : undefined;
    }

    /** Iniciales del nombre, para mostrar cuando no hay foto. */
    get iniciales(): string {
        return this.nombre.split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
    }

    /** Al llegar arriba del historial, carga los mensajes anteriores. */
    alScroll(): void {
        const el = this.scroller?.nativeElement;
        if (el && el.scrollTop <= 40 && this.hayAnteriores && !this.cargandoAnteriores) {
            this.cargarAnteriores();
        }
    }

    /** Carga otra página de mensajes anteriores conservando la posición del scroll. */
    cargarAnteriores(): void {
        const el = this.scroller?.nativeElement;
        if (!el || this.cargandoAnteriores || !this.hayAnteriores) {
            return;
        }
        this.cargandoAnteriores = true;
        this.temporizador = setTimeout(() => {
            const alturaPrevia = el.scrollHeight;
            const topPrevio = el.scrollTop;
            this.cantidadVisible += TAMANO_PAGINA;
            this.cargandoAnteriores = false;
            this.cdr.detectChanges();
            el.scrollTop = el.scrollHeight - alturaPrevia + topPrevio;
        }, 400);
    }

    /** Enter envía el mensaje; Shift+Enter agrega una línea. */
    enter(evento: Event): void {
        if ((evento as KeyboardEvent).shiftKey) {
            return;
        }
        evento.preventDefault();
        this.enviarTexto();
    }

    /** Emite el texto escrito y limpia la caja. */
    enviarTexto(): void {
        const caja = this.caja?.nativeElement;
        const contenido = (caja?.value ?? '').trim();
        if (!caja || !contenido) {
            return;
        }
        this.enviar.emit(contenido);
        caja.value = '';
        this.texto = '';
        caja.focus();
    }

    /** Lleva el scroll al último mensaje. */
    private irAbajo(): void {
        setTimeout(() => {
            const el = this.scroller?.nativeElement;
            if (el) {
                el.scrollTop = el.scrollHeight;
            }
        });
    }
}
