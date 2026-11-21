import {inject, NgModule} from '@angular/core';
import {CanActivateFn, Router, RouterModule, Routes} from '@angular/router';
import {AuthService} from './services/authservice';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';
import {Contactoscomponent} from "./components/contactoscomponent/contactoscomponent";
import {Errorcomponent} from "./components/errorcomponent/errorcomponent";

/** Solo deja entrar si hay sesión iniciada; si no, manda al login. */
const sesionRequerida: CanActivateFn = () =>
    inject(AuthService).estaLogueado() || inject(Router).createUrlTree(['/login']);

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent, canActivate: [sesionRequerida] },
    { path: 'admin', component: Admincomponent, canActivate: [sesionRequerida] },
    { path: 'confirm-email', component: Confirmaremailcomponent },
    { path: 'chats', redirectTo: 'principal', pathMatch: 'full' },
    { path: 'contactos', component: Contactoscomponent, canActivate: [sesionRequerida] },
    { path: 'error/:codigo', component: Errorcomponent },
    { path: '**', redirectTo: 'error/404' },
];

/** Rutas de la aplicación: login, principal, admin, contactos, confirmar correo y páginas de error. */
@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}