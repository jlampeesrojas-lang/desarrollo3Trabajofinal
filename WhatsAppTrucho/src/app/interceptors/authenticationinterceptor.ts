import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { AuthService } from '../services/authservice';


export function authenticationInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
    const auth = inject(AuthService);
    const router = inject(Router);
    const esDeAuth = req.url.indexOf('/auth/') !== -1;
    const token = auth.obtenerToken();

    let peticion = req;
    if (token != null && !esDeAuth) {
        peticion = req.clone({
            setHeaders: { Authorization: 'Bearer ' + token },
        });
    }

    return next(peticion).pipe(
        catchError(function (e: HttpErrorResponse) {
            if (!esDeAuth && e.status === 401) {
                auth.cerrarSesion();
                router.navigate(['/login']);
            }
            if (!esDeAuth && e.status === 403) {
                if (token == null || auth.tokenVencido()) {
                    auth.cerrarSesion();
                    router.navigate(['/login']);
                } else {
                    alert('No tienes permiso para hacer esto');
                }
            }
            return throwError(function () {
                return e;
            });
        })
    );
}