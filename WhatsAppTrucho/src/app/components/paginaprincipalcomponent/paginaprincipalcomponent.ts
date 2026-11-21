import {ContactService} from "../../services/contactservice";
import {Contacto} from "../../models/contacto";
import {GifService} from "../../services/gifservice";
import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../../services/authservice';
import {Gif} from "../../models/gif";

/** Página principal: lista de chats, conversación, adjuntos, GIFs y audio. */
@Component({
    selector: 'app-paginaprincipalcomponent',
    standalone: false,
    styleUrl: './paginaprincipalcomponent.css',
    templateUrl: './paginaprincipalcomponent.html',
})
export class Paginaprincipalcomponent implements OnInit {
    seccion: 'chats' | 'ia' | 'contactos' = 'chats';
    contactos: Contacto[] = [];
    seleccionado: Contacto | null = null;
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

    /**
     * Inicializa los servicios de autenticación y navegación.
     */
    constructor(
        private contactosApi: ContactService,
        private auth: AuthService,
        private gifsApi: GifService,
        private cdr: ChangeDetectorRef,
        private router: Router
    ) {
    }

    /**
     * Obtiene el nombre del usuario al iniciar el componente.
     */
    ngOnInit(): void {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(['/error', '401']);
            return;
        }
        this.miUsuario = this.auth.obtenerUsuario() || 'Usuario';
        this.esAdmin = this.auth.obtenerRol() === 'ADMIN';
        this.contactosApi.listarContactos().subscribe({
            next: lista => {
                this.contactos = lista;
                this.cdr.markForCheck();
            },
            error: fallo => {
                if (fallo.status === 401) this.auth.avisarVencimiento();
                else this.avisoContactos = 'No se pudieron cargar los contactos.';
                this.cdr.markForCheck();
            }
        });
    }

    /** Busca GIF con el texto ingresado y actualiza los resultados del chat. */
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
                if (fallo.status === 401) this.auth.avisarVencimiento();
                else this.avisoGif = typeof fallo.error === 'string' ? fallo.error : 'No se pudieron cargar los GIFs.';
                this.buscandoGif = false;
                this.cdr.markForCheck();
            }
        });
    }

    /**
     * Cambia la sección que se muestra en la página principal.
     */
    irASeccion(
        seccion: 'chats' | 'ia' | 'contactos'
    ): void {
        this.seccion = seccion;
        this.mensajeContacto = '';
        this.errorContacto = '';
    }

    /**
     * Cierra la sesión y redirige al usuario al inicio de sesión.
     */
    salir(): void {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }

    /**
     * Abre la pantalla de contactos.
     */
    irAContactos(): void {
        this.router.navigate(['/contactos']);
    }

    /**
     * Abre el panel de administración (solo se muestra al administrador).
     */
    irAAdmin(): void {
        this.router.navigate(['/admin']);
    }
}