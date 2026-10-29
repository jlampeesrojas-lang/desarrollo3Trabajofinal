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
    edadMinima = 18;
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
        if (typeof e.error === 'string' && e.error !== '') {
            return e.error;
        }
        if (e.error && e.error.mensaje) {
            return e.error.mensaje;
        }
        if (e.status === 0) {
            return 'No se pudo conectar con el servidor';
        }
        if (e.status === 403) {
            return 'No tienes permiso para hacer esto';
        }
        if (e.status >= 500) {
            return 'Error del servidor, intenta más tarde';
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
                if (componente.auth.obtenerRol() === 'ADMIN') {
                    componente.router.navigate(['/admin']);
                } else {
                    componente.router.navigate(['/principal']);
                }
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
        const errorClave = this.validarClave(r.clave);
        if (errorClave !== '') {
            this.error = errorClave;
            return;
        }

        const edad = this.calcularEdad(r.fechaNacimiento);
        if (edad < 0 || edad > 120) {
            this.error = 'La fecha de nacimiento no es válida';
            return;
        }
        if (edad < this.edadMinima) {
            this.error = 'Debes tener al menos ' + this.edadMinima + ' años para crear una cuenta';
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

        const errorClave = this.validarClave(this.nueva.clave);
        if (errorClave !== '') {
            this.error = errorClave;
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

    validarClave(clave: string): string {
        if (clave.length < 12) {
            return 'La contraseña debe tener mínimo 12 caracteres';
        }
        if (!/[A-Z]/.test(clave)) {
            return 'La contraseña debe tener al menos una mayúscula';
        }
        if (!/[0-9]/.test(clave)) {
            return 'La contraseña debe tener al menos un número';
        }
        if (!/[^A-Za-z0-9]/.test(clave)) {
            return 'La contraseña debe tener al menos un símbolo';
        }
        return '';
    }

    calcularEdad(fecha: string): number {
        const partes = fecha.split('-');
        const anio = Number(partes[0]);
        const mes = Number(partes[1]);
        const dia = Number(partes[2]);

        const hoy = new Date();
        let edad = hoy.getFullYear() - anio;
        const mesActual = hoy.getMonth() + 1;
        if (mesActual < mes || (mesActual === mes && hoy.getDate() < dia)) {
            edad = edad - 1;
        }
        return edad;
    }
}