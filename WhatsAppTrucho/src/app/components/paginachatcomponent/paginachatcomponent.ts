import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { Chat, EstadoConexion, Mensaje, Participante } from '../../models/chat';

@Component({
    selector: 'app-paginachatcomponent',
    standalone: false,
    templateUrl: './paginachatcomponent.html',
    styleUrl: './paginachatcomponent.css',
})
export class Paginachatcomponent implements OnDestroy {
    miId = 1;
    yo: Participante = { id: 1, nombre: 'Tú' };
    ia: Participante = { id: 100, nombre: 'IA', avatarUrl: 'assets/ia.jpg' };

    chats: Chat[] = [
        { id: 1, codigo: 'CH-IA0001', participantes: [this.yo, this.ia], ultimoMensaje: null, noLeidos: 0 },
    ];
    mensajesPorChat: { [id: number]: Mensaje[] } = { 1: [] };

    chatActivoId: number | null = null;
    conexion: EstadoConexion = 'CONECTADO';
    conexionRestablecida = false;

    private contador = 0;
    private temporizadores: ReturnType<typeof setTimeout>[] = [];

    constructor(private cdr: ChangeDetectorRef) {}

    ngOnDestroy(): void {
        this.temporizadores.forEach(temporizador => clearTimeout(temporizador));
    }

    get chatActivo(): Chat | null {
        return this.chats.find(c => c.id === this.chatActivoId) ?? null;
    }

    get mensajesActivos(): Mensaje[] {
        return this.chatActivoId === null ? [] : this.mensajesPorChat[this.chatActivoId] ?? [];
    }

    seleccionarChat(id: number): void {
        this.chatActivoId = id;
        this.actualizarChat(id, { noLeidos: 0 });
    }

    volver(): void {
        this.chatActivoId = null;
    }

    enviar(texto: string): void {
        const id = this.chatActivoId;
        if (id === null) {
            return;
        }
        const mensaje = this.crearMensaje(id, this.yo, texto, 'ENVIANDO');
        this.agregarMensaje(mensaje);
        this.transmitir(mensaje);
    }

    reintentar(mensaje: Mensaje): void {
        this.cambiarEstado(mensaje.id, mensaje.chatId, 'ENVIANDO');
        this.transmitir({ ...mensaje, estado: 'ENVIANDO' });
    }

    simularCaida(): void {
        if (this.conexion !== 'CONECTADO') {
            return;
        }
        this.conexion = 'RECONECTANDO';
        this.conexionRestablecida = false;
        this.programar(() => {
            this.conexion = 'CONECTADO';
            this.conexionRestablecida = true;
            this.programar(() => (this.conexionRestablecida = false), 3000);
        }, 4000);
    }

    private transmitir(mensaje: Mensaje): void {
        this.programar(() => {
            const fallo = this.conexion !== 'CONECTADO' || mensaje.contenido.includes('#error');
            if (fallo) {
                this.cambiarEstado(mensaje.id, mensaje.chatId, 'ERROR');
                return;
            }
            this.cambiarEstado(mensaje.id, mensaje.chatId, 'ENVIADO');
        }, 600);
    }

    private crearMensaje(chatId: number, autor: Participante, contenido: string, estado: Mensaje['estado']): Mensaje {
        this.contador++;
        return {
            id: `m-${this.contador}`,
            chatId,
            remitenteId: autor.id,
            remitenteNombre: autor.nombre,
            contenido,
            fechaEnvio: new Date().toISOString(),
            estado,
        };
    }

    private agregarMensaje(mensaje: Mensaje): void {
        const lista = [...(this.mensajesPorChat[mensaje.chatId] ?? []), mensaje];
        this.mensajesPorChat = { ...this.mensajesPorChat, [mensaje.chatId]: lista };
        const chat = this.chats.find(c => c.id === mensaje.chatId);
        const noLeido = mensaje.remitenteId !== this.miId && mensaje.chatId !== this.chatActivoId;
        this.actualizarChat(mensaje.chatId, {
            ultimoMensaje: mensaje,
            noLeidos: (chat?.noLeidos ?? 0) + (noLeido ? 1 : 0),
        });
    }

    private cambiarEstado(idMensaje: string, chatId: number, estado: Mensaje['estado']): void {
        const lista = (this.mensajesPorChat[chatId] ?? []).map(m => (m.id === idMensaje ? { ...m, estado } : m));
        this.mensajesPorChat = { ...this.mensajesPorChat, [chatId]: lista };
        const ultimo = lista[lista.length - 1] ?? null;
        this.actualizarChat(chatId, { ultimoMensaje: ultimo });
    }

    private actualizarChat(id: number, cambios: Partial<Chat>): void {
        this.chats = this.chats.map(c => (c.id === id ? { ...c, ...cambios } : c));
    }

    private programar(accion: () => void, ms: number): void {
        const temporizador = setTimeout(() => {
            accion();
            this.cdr.detectChanges();
        }, ms);
        this.temporizadores.push(temporizador);
    }
}
