
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/authservice';

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

    constructor(
        private auth: AuthService,
        private router: Router
    ) {}

    
    ngOnInit(): void {
        this.miUsuario = this.auth.obtenerUsuario() || 'Usuario';
    }


    irASeccion(
        seccion: 'chats' | 'ia' | 'contactos'
    ): void {
        this.seccion = seccion;

        this.mensajeContacto = '';
        this.errorContacto = '';
    }


    salir(): void {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }
}