import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/authservice';

export function adminGuard(): boolean {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.estaLogueado() && auth.obtenerRol() === 'ADMIN') {
        return true;
    }
    router.navigate(['/principal']);
    return false;
}