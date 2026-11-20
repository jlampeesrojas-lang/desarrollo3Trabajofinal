import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';

/** Botón para grabar un audio con el micrófono y escucharlo antes de enviarlo. */
@Component({
    selector: 'app-audiocomponent',
    standalone: false,
    templateUrl: './audiocomponent.html',
    styleUrl: './audiocomponent.css'
})
export class Audiocomponent implements OnDestroy {
    grabando = false;
    ocupado = false;
    audioUrl = '';
    aviso = '';

    private grabadora?: MediaRecorder;
    private microfono?: MediaStream;
    private destruido = false;

    constructor(private cambios: ChangeDetectorRef) {}

    /** Empieza o termina la grabación según el estado actual del micrófono. */
    pulsarMicrofono() {
        if (this.ocupado) return;

        if (this.grabando && this.grabadora?.state === 'recording') {
            this.ocupado = true;
            this.grabadora.stop();
            return;
        }

        this.aviso = '';
        this.ocupado = true;
        if (!navigator.mediaDevices) {
            this.errorGrabacion();
            return;
        }

        navigator.mediaDevices.getUserMedia({ audio: true })
            .then(this.iniciarGrabacion.bind(this))
            .catch(this.errorGrabacion.bind(this));
    }

    /** Arranca la grabadora con el micrófono ya autorizado. */
    private iniciarGrabacion(microfono: MediaStream) {
        this.microfono = microfono;
        if (this.destruido) {
            this.liberarMicrofono();
            return;
        }

        const grabadora = new MediaRecorder(microfono);
        const partes: Blob[] = [];
        this.grabadora = grabadora;

        grabadora.ondataavailable = function(evento) {
            if (evento.data.size > 0) partes.push(evento.data);
        };
        grabadora.onstop = this.terminar.bind(this, partes, grabadora);
        grabadora.onerror = this.errorGrabacion.bind(this);
        grabadora.start();
        this.grabando = true;
        this.ocupado = false;
        this.cambios.markForCheck();
    }

    /** Une las partes grabadas en un audio y lo deja listo para escuchar. */
    private terminar(partes: Blob[], grabadora: MediaRecorder) {
        if (this.destruido || this.grabadora !== grabadora) return;
        this.liberarMicrofono();
        if (partes.length > 0) {
            this.borrar();
            this.audioUrl = URL.createObjectURL(new Blob(partes, { type: grabadora.mimeType }));
        } else {
            this.aviso = 'El micrófono no capturó audio. Intenta otra vez.';
        }
        this.cambios.markForCheck();
    }

    /** Avisa que no se pudo grabar y libera el micrófono. */
    private errorGrabacion() {
        this.liberarMicrofono();
        this.aviso = 'No se pudo grabar. Revisa el permiso del micrófono.';
        if (!this.destruido) this.cambios.markForCheck();
    }

    /** Detiene el micrófono para que el navegador deje de usarlo. */
    private liberarMicrofono() {
        if (this.microfono) {
            for (const pista of this.microfono.getTracks()) pista.stop();
        }
        this.microfono = undefined;
        this.grabadora = undefined;
        this.grabando = false;
        this.ocupado = false;
    }

    /** Descarta la grabación actual. */
    borrar() {
        if (this.audioUrl) URL.revokeObjectURL(this.audioUrl);
        this.audioUrl = '';
    }

    /** Se ejecuta al destruir el componente y libera el micrófono. */
    ngOnDestroy() {
        this.destruido = true;
        this.liberarMicrofono();
        this.borrar();
    }
}