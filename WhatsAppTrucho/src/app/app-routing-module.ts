import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';
import {Contactoscomponent} from "./components/contactoscomponent/contactoscomponent";
import {Errorcomponent} from "./components/errorcomponent/errorcomponent";

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent},
    { path: 'admin', component: Admincomponent },
    { path: 'confirm-email', component: Confirmaremailcomponent },
    { path: 'chats', redirectTo: 'principal', pathMatch: 'full' },
    { path: 'contactos', component: Contactoscomponent },
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