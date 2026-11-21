import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './services/authservice';

/**
 * Componente raíz de la aplicación.
 * Contiene la zona de rutas y el aviso que se muestra cuando la sesión vence.
 */
@Component({
    selector: 'app-root',
    standalone: false,
    templateUrl: './app.html',
    styleUrl: './app.css',
})
export class App {

    /** Título de la aplicación. */
    protected readonly title = signal('untitled2');

    /**
     * Inicializa los servicios de autenticación y navegación.
     *
     * @param auth   Servicio de autenticación. Es público porque la vista lo usa
     *               para saber si la sesión venció.
     * @param router Servicio de navegación entre rutas.
     */
    constructor(
        public auth: AuthService,
        private router: Router
    ) {}

    /**
     * Obtiene el nombre del usuario que inició sesión.
     *
     * @returns El nombre del usuario, o 'Usuario' si no hay sesión activa.
     */
    get miUsuario(): string {
        return this.auth.obtenerUsuario() || 'Usuario';
    }

    /**
     * Cierra la sesión del usuario y lo redirige a la pantalla de inicio de sesión.
     */
    salir(): void {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }

    /**
     * Lleva al usuario a la pantalla de inicio de sesión cuando su sesión venció.
     */
    irAlLogin(): void {
        this.salir();
    }
}