import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../services/authservice';

/** Pantalla a la que llega el enlace del correo para confirmar la cuenta. */
@Component({
    selector: 'app-confirmaremail',
    standalone: false,
    templateUrl: './confirmaremailcomponent.html',
    styleUrl: './confirmaremailcomponent.css',
})
export class Confirmaremailcomponent implements OnInit {
    estado = 'cargando';
    mensaje = '';

    constructor(private route: ActivatedRoute, private router: Router,
                private auth: AuthService, private cdr: ChangeDetectorRef) {
    }

    /** Se ejecuta al iniciar el componente y carga los datos iniciales. */
    ngOnInit() {
        const token = this.route.snapshot.queryParamMap.get('token');
        if (token == null || token === '') {
            this.estado = 'error';
            this.mensaje = 'El enlace no es válido';
            return;
        }

        this.auth.confirmarCorreo(token).subscribe({
            next: () => {
                this.estado = 'ok';
                this.mensaje = 'Correo confirmado. Ya puedes iniciar sesión.';
                this.cdr.markForCheck();
            },
            error: (e: HttpErrorResponse) => {
                this.estado = 'error';
                if (e.error && e.error.mensaje) {
                    this.mensaje = e.error.mensaje;
                } else {
                    this.mensaje = 'No se pudo confirmar el correo';
                }
                this.cdr.markForCheck();
            },
        });
    }

    /** Lleva al usuario a la pantalla de inicio de sesión. */
    irAlLogin() {
        this.router.navigate(['/login']);
    }
}