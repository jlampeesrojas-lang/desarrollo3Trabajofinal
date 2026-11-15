import { ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import { Chat, EstadoConexion, Mensaje } from '../../models/chat';

const TAMANO_PAGINA = 20;

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

    @Output() enviar = new EventEmitter<string>();
    @Output() reintentar = new EventEmitter<Mensaje>();
    @Output() volver = new EventEmitter<void>();
    @Output() simularCaida = new EventEmitter<void>();

    @ViewChild('scroller') scroller?: ElementRef<HTMLElement>;
    @ViewChild('caja') caja?: ElementRef<HTMLTextAreaElement>;

    cantidadVisible = TAMANO_PAGINA;
    cargandoAnteriores = false;
    foto = true;
    texto = '';

    private temporizador: ReturnType<typeof setTimeout> | null = null;

    constructor(private cdr: ChangeDetectorRef) {}

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

    ngOnDestroy(): void {
        if (this.temporizador) {
            clearTimeout(this.temporizador);
        }
    }

    get visibles(): Mensaje[] {
        return this.mensajes.slice(-this.cantidadVisible);
    }

    get hayAnteriores(): boolean {
        return this.mensajes.length > this.cantidadVisible;
    }

    get nombre(): string {
        const otros = this.chat.participantes.filter(p => p.id !== this.miId).map(p => p.nombre);
        return otros.length ? otros.join(', ') : 'Chat sin participantes';
    }

    get avatar(): string | undefined {
        return this.foto ? this.chat.participantes.find(p => p.id !== this.miId)?.avatarUrl : undefined;
    }

    get iniciales(): string {
        return this.nombre.split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
    }

    alScroll(): void {
        const el = this.scroller?.nativeElement;
        if (el && el.scrollTop <= 40 && this.hayAnteriores && !this.cargandoAnteriores) {
            this.cargarAnteriores();
        }
    }

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

    enter(evento: Event): void {
        if ((evento as KeyboardEvent).shiftKey) {
            return;
        }
        evento.preventDefault();
        this.enviarTexto();
    }

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

    private irAbajo(): void {
        setTimeout(() => {
            const el = this.scroller?.nativeElement;
            if (el) {
                el.scrollTop = el.scrollHeight;
            }
        });
    }
}
