import { ChangeDetectorRef, Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/authservice';
import { RegistroRequest, LoginResponse } from '../../models/authentication';

@Component({
    selector: 'app-login',
    standalone: false,
    templateUrl: './logincomponent.html',
    styleUrl: './logincomponent.css',
})
export class Logincomponent {
    vista = 'login';
    cargando = false;
    error = '';
    exito = '';

    usuario = '';
    password = '';

    registro: RegistroRequest = {
        nombreCompleto: '',
        nombreUsuario: '',
        correo: '',
        telefono: '',
        fechaNacimiento: '',
        clave: '',
        confirmarClave: '',
    };
    codigoRegistro = '';

    correoRecuperar = '';
    codigoRecuperar = '';
    nueva = { clave: '', confirmar: '' };

    constructor(private router: Router, private auth: AuthService, private cdr: ChangeDetectorRef) {}

    irA(vista: string) {
        this.vista = vista;
        this.error = '';
        this.exito = '';
    }

    mensajeDeError(e: any, mensajePorDefecto: string): string {
        if (e.error && e.error.mensaje) {
            return e.error.mensaje;
        }
        return mensajePorDefecto;
    }

    iniciarSesion() {
        if (this.usuario === '' || this.password === '') {
            this.error = 'Completa todos los campos';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.login({ usuario: this.usuario, password: this.password }).subscribe({
            next: function (respuesta: LoginResponse) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.auth.guardarToken(respuesta.token);
                componente.router.navigate(['/principal']);
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'Usuario o contraseña incorrectos');
            },
        });
    }

    crearCuenta() {
        const r = this.registro;

        if (r.nombreCompleto === '' || r.nombreUsuario === '' || r.correo === '' ||
            r.telefono === '' || r.fechaNacimiento === '' || r.clave === '' ||
            r.confirmarClave === '') {
            this.error = 'Completa todos los campos';
            return;
        }
        if (r.clave !== r.confirmarClave) {
            this.error = 'Las contraseñas no coinciden';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.registro(r).subscribe({
            next: function () {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.irA('verificacion');
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'No se pudo crear la cuenta');
            },
        });
    }

    verificarRegistro() {
        if (this.codigoRegistro === '') {
            this.error = 'Escribe el código';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.verificarRegistro({
            correo: this.registro.correo,
            codigo: this.codigoRegistro,
        }).subscribe({
            next: function () {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.irA('login');
                componente.exito = 'Cuenta creada, ya puedes iniciar sesión';
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'Código incorrecto o vencido');
            },
        });
    }

    enviarCodigoRecuperar() {
        if (this.correoRecuperar === '') {
            this.error = 'Escribe tu correo';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.pedirCodigoRecuperar({ correo: this.correoRecuperar }).subscribe({
            next: function () {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.irA('codigoRecuperar');
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'No se pudo enviar el código');
            },
        });
    }

    verificarCodigoRecuperar() {
        if (this.codigoRecuperar === '') {
            this.error = 'Escribe el código';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.verificarCodigoRecuperar({
            correo: this.correoRecuperar,
            codigo: this.codigoRecuperar,
        }).subscribe({
            next: function () {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.irA('nuevaClave');
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'Código incorrecto o vencido');
            },
        });
    }

    cambiarClave() {
        if (this.nueva.clave === '' || this.nueva.confirmar === '') {
            this.error = 'Completa todos los campos';
            return;
        }
        if (this.nueva.clave !== this.nueva.confirmar) {
            this.error = 'Las contraseñas no coinciden';
            return;
        }

        const componente = this;
        this.error = '';
        this.cargando = true;

        this.auth.cambiarClave({
            correo: this.correoRecuperar,
            codigo: this.codigoRecuperar,
            clave: this.nueva.clave,
        }).subscribe({
            next: function () {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.irA('login');
                componente.exito = 'Contraseña cambiada, ya puedes iniciar sesión';
            },
            error: function (e: any) {
                componente.cargando = false;
                componente.cdr.markForCheck();
                componente.error = componente.mensajeDeError(e, 'No se pudo cambiar la contraseña');
            },
        });
    }
}