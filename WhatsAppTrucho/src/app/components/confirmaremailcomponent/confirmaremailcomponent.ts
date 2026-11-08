import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../services/authservice';

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
            error: (e: any) => {
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

    irAlLogin() {
        this.router.navigate(['/login']);
    }
}