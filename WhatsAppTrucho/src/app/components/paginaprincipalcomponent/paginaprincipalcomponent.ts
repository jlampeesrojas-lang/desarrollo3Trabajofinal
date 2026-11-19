import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../../services/authservice';

@Component({
    selector: 'app-paginaprincipalcomponent',
    standalone: false,
    styleUrl: './paginaprincipalcomponent.css',
    templateUrl: './paginaprincipalcomponent.html',
})
export class Paginaprincipalcomponent implements OnInit {

    seccion: 'chats' | 'ia' | 'contactos' = 'chats';

    miUsuario = 'Usuario';

    busquedaChat = '';
    mensajeNuevo = '';

    preguntaIA = '';

    busquedaContacto = '';
    mensajeContacto = '';
    errorContacto = '';

    /**
     * Inicializa los servicios de autenticación y navegación.
     */
    constructor(
        private auth: AuthService,
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
    }

    /**
     * Cambia la sección que se muestra en la página principal.
     * @param seccion Sección a la que se desea navegar.
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
}
