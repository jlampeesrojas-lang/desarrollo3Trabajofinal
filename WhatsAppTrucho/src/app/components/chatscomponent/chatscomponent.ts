import {Component, OnInit} from '@angular/core';
import {AuthService} from '../../services/authservice';
import {ChatService, Conversacion, AdjuntoChat} from '../../services/chatservice';

@Component({
    selector: 'app-chatscomponent',
    standalone: false,
    templateUrl: './chatscomponent.html',
    styleUrl: './chatscomponent.css'
})
export class Chatscomponent implements OnInit {

    chats: Conversacion[] = [];
    cargando = true;
    enviando = false;
    error = '';
    seleccionado = '';
    busqueda = '';
    usuario = '';

    borradores: Record<string, { texto: string, archivos: File[] }> = {};

    constructor(
        private servicio: ChatService,
        private auth: AuthService
    ) {
    }

    async ngOnInit() {
        this.usuario = this.auth.obtenerUsuario() || 'Usuario';

        try {
            this.chats = await this.servicio.cargar(this.usuario);
        } catch {
            this.error = 'No se pudieron cargar los chats';
        }

        this.cargando = false;
    }

    get chat() {
        return this.chats.find(c => c.id === this.seleccionado);
    }

    get filtrados() {
        return this.chats.filter(c =>
            c.nombre.toLowerCase().includes(this.busqueda.toLowerCase())
        );
    }

    get borrador() {
        if (!this.borradores[this.seleccionado]) {
            this.borradores[this.seleccionado] = {
                texto: '',
                archivos: []
            };
        }

        return this.borradores[this.seleccionado];
    }

    adjuntar(event: Event) {
        const input = event.target as HTMLInputElement;

        if (input.files) {
            for (let i = 0; i < input.files.length; i++) {
                this.borrador.archivos.push(input.files[i]);
            }
        }

        input.value = '';
    }

    async enviar() {
        if (!this.chat || this.enviando) {
            return;
        }

        const texto = this.borrador.texto.trim();

        if (texto === '' && this.borrador.archivos.length === 0) {
            return;
        }

        const adjuntos: AdjuntoChat[] = [];

        for (let archivo of this.borrador.archivos) {
            adjuntos.push({
                nombre: archivo.name,
                archivo: archivo
            });
        }

        const mensaje = {
            id: crypto.randomUUID(),
            texto: texto,
            enviado: true,
            fecha: new Date().toISOString(),
            adjuntos: adjuntos
        };

        this.enviando = true;

        try {
            const actualizados = this.chats.map(c => {
                if (c.id === this.seleccionado) {
                    return {
                        ...c,
                        mensajes: [...c.mensajes, mensaje]
                    };
                }
                return c;
            });

            await this.servicio.guardar(this.usuario, actualizados);

            this.chats = actualizados;
            this.borradores[this.seleccionado] = {
                texto: '',
                archivos: []
            };

            this.error = '';

        } catch {
            this.error = 'No se pudo enviar el mensaje';
        }

        this.enviando = false;
    }

    descargar(adjunto: AdjuntoChat) {
        const url = URL.createObjectURL(adjunto.archivo);
        const enlace = document.createElement('a');

        enlace.href = url;
        enlace.download = adjunto.nombre;
        enlace.click();

        URL.revokeObjectURL(url);
    }
}