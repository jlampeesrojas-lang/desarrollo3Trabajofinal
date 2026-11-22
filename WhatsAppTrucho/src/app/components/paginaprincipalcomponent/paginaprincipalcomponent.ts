import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Subscription} from 'rxjs';

import {Contacto} from '../../models/contacto';
import {Gif} from '../../models/gif';
import {AuthService} from '../../services/authservice';
import {ContactService} from '../../services/contactservice';
import {GifService} from '../../services/gifservice';

/** Muestra las conversaciones, los contactos y el acceso a administración. */
@Component({
    selector: 'app-paginaprincipalcomponent',
    standalone: false,
    styleUrl: './paginaprincipalcomponent.css',
    templateUrl: './paginaprincipalcomponent.html',
})
export class Paginaprincipalcomponent implements OnInit, OnDestroy {
    seccion: 'chats' | 'ia' | 'contactos' = 'chats';
    contactos: Contacto[] = [];
    seleccionado: Contacto | null = null;
    chatAbiertoEnMovil = false;
    avisoContactos = '';
    miUsuario = 'Usuario';
    busquedaChat = '';
    mensajeNuevo = '';
    gifs: Gif[] = [];
    buscandoGif = false;
    avisoGif = '';
    preguntaIA = '';
    esAdmin = false;

    busquedaContacto = '';
    mensajeContacto = '';
    errorContacto = '';

    private temporizadorContactos?: ReturnType<typeof setTimeout>;
    private solicitudContactos?: Subscription;

    /** Recibe los servicios de sesión, contactos, GIF y navegación. */
    constructor(
        private contactosApi: ContactService,
        private auth: AuthService,
        private gifsApi: GifService,
        private cdr: ChangeDetectorRef,
        private router: Router
    ) {
    }

    /** Comprueba la sesión, identifica al administrador y carga los contactos. */
    ngOnInit(): void {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(['/error', '401']);
            return;
        }

        this.miUsuario = this.auth.obtenerUsuario() || 'Usuario';
        this.esAdmin = this.auth.obtenerRol() === 'ADMIN';
        this.cargarContactos();
    }

    /** Abre el contacto seleccionado o la conversación con la IA. */
    abrirConversacion(contacto: Contacto | null): void {
        this.seleccionado = contacto;
        this.chatAbiertoEnMovil = true;
    }

    /** Vuelve a la lista de chats en la vista móvil. */
    volverAChats(): void {
        this.chatAbiertoEnMovil = false;
    }

    /** Actualiza los contactos de la barra lateral. */
    private cargarContactos(): void {
        this.solicitudContactos = this.contactosApi.listarContactos().subscribe({
            next: lista => {
                this.contactos = lista;
                this.avisoContactos = '';
                this.cdr.markForCheck();
                this.programarCargaContactos();
            },
            error: fallo => {
                if (fallo.status === 401) {
                    this.auth.avisarVencimiento();
                    return;
                }

                this.avisoContactos = 'No se pudieron actualizar los contactos.';
                this.cdr.markForCheck();
                this.programarCargaContactos();
            }
        });
    }

    /** Espera dos segundos antes de actualizar los contactos otra vez. */
    private programarCargaContactos(): void {
        this.temporizadorContactos = setTimeout(() => this.cargarContactos(), 2000);
    }

    /** Cancela la actualización de contactos al salir de la pantalla. */
    ngOnDestroy(): void {
        clearTimeout(this.temporizadorContactos);
        this.solicitudContactos?.unsubscribe();
    }

    /** Busca GIF con el texto ingresado. */
    buscarGifs(texto: string): void {
        if (!this.auth.estaLogueado()) return;

        const query = texto.trim();
        if (!query || this.buscandoGif) return;

        this.buscandoGif = true;
        this.gifs = [];
        this.avisoGif = '';

        this.gifsApi.buscar(query).subscribe({
            next: lista => {
                this.gifs = lista;
                this.avisoGif = lista.length ? '' : 'No se encontraron GIFs.';
                this.buscandoGif = false;
                this.cdr.markForCheck();
            },
            error: fallo => {
                if (fallo.status === 401) {
                    this.auth.avisarVencimiento();
                } else {
                    this.avisoGif = typeof fallo.error === 'string'
                        ? fallo.error
                        : 'No se pudieron cargar los GIFs.';
                }

                this.buscandoGif = false;
                this.cdr.markForCheck();
            }
        });
    }

    /** Cambia la sección que se muestra en la página principal. */
    irASeccion(seccion: 'chats' | 'ia' | 'contactos'): void {
        this.seccion = seccion;
        this.mensajeContacto = '';
        this.errorContacto = '';
    }

    /** Cierra la sesión y vuelve al inicio de sesión. */
    salir(): void {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }

    /** Abre la pantalla de contactos. */
    irAContactos(): void {
        this.router.navigate(['/contactos']);
    }

    /** Abre el panel de administración. */
    irAAdmin(): void {
        this.router.navigate(['/admin']);
    }

    /** Abre la pantalla de información de la empresa y preguntas frecuentes. */
    irAInformacion(): void {
        this.router.navigate(['/informacion']);
    }
}