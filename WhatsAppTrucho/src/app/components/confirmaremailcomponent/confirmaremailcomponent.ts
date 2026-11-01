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

        const componente = this;
        this.auth.confirmarCorreo(token).subscribe({
            next: function () {
                componente.estado = 'ok';
                componente.mensaje = 'Correo confirmado. Ya puedes iniciar sesión.';
                componente.cdr.markForCheck();
            },
            error: function (e: any) {
                componente.estado = 'error';
                if (e.error && e.error.mensaje) {
                    componente.mensaje = e.error.mensaje;
                } else {
                    componente.mensaje = 'No se pudo confirmar el correo';
                }
                componente.cdr.markForCheck();
            },
        });
    }

    irAlLogin() {
        this.router.navigate(['/login']);
    }
}