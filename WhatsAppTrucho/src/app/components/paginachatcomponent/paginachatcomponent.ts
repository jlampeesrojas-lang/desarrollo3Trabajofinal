import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { Chat, EstadoConexion, Mensaje, Participante } from '../../models/chat';

/** Página del chat: guarda los datos simulados y conecta la lista con la ventana. */
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

    /** Cancela los temporizadores pendientes al destruir la página. */
    ngOnDestroy(): void {
        this.temporizadores.forEach(temporizador => clearTimeout(temporizador));
    }

    /** Chat seleccionado, o null si no hay ninguno. */
    get chatActivo(): Chat | null {
        return this.chats.find(c => c.id === this.chatActivoId) ?? null;
    }

    /** Mensajes del chat seleccionado. */
    get mensajesActivos(): Mensaje[] {
        return this.chatActivoId === null ? [] : this.mensajesPorChat[this.chatActivoId] ?? [];
    }

    /** Abre un chat y lo marca como leído. */
    seleccionarChat(id: number): void {
        this.chatActivoId = id;
        this.actualizarChat(id, { noLeidos: 0 });
    }

    /** Vuelve a la lista de chats (vista móvil). */
    volver(): void {
        this.chatActivoId = null;
    }

    /** Crea el mensaje en estado ENVIANDO y lo transmite. */
    enviar(texto: string): void {
        const id = this.chatActivoId;
        if (id === null) {
            return;
        }
        const mensaje = this.crearMensaje(id, this.yo, texto, 'ENVIANDO');
        this.agregarMensaje(mensaje);
        this.transmitir(mensaje);
    }

    /** Reenvía un mensaje que quedó en ERROR. */
    reintentar(mensaje: Mensaje): void {
        this.cambiarEstado(mensaje.id, mensaje.chatId, 'ENVIANDO');
        this.transmitir({ ...mensaje, estado: 'ENVIANDO' });
    }

    /** Simula una desconexión de 4 segundos y luego la reconexión. */
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

    /** Simula el envío: pasa a ENVIADO, o a ERROR si no hay conexión o el texto contiene #error. */
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

    /** Construye un mensaje nuevo con id incremental. */
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

    /** Agrega el mensaje al chat y actualiza el último mensaje y los no leídos. */
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

    /** Cambia el estado de envío de un mensaje. */
    private cambiarEstado(idMensaje: string, chatId: number, estado: Mensaje['estado']): void {
        const lista = (this.mensajesPorChat[chatId] ?? []).map(m => (m.id === idMensaje ? { ...m, estado } : m));
        this.mensajesPorChat = { ...this.mensajesPorChat, [chatId]: lista };
        const ultimo = lista[lista.length - 1] ?? null;
        this.actualizarChat(chatId, { ultimoMensaje: ultimo });
    }

    /** Aplica cambios a un chat sin mutar el arreglo original. */
    private actualizarChat(id: number, cambios: Partial<Chat>): void {
        this.chats = this.chats.map(c => (c.id === id ? { ...c, ...cambios } : c));
    }

    /** Programa una acción simulada y refresca la vista. */
    private programar(accion: () => void, ms: number): void {
        const temporizador = setTimeout(() => {
            accion();
            this.cdr.detectChanges();
        }, ms);
        this.temporizadores.push(temporizador);
    }
}
