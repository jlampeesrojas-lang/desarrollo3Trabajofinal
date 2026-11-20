import {inject, NgModule} from '@angular/core';
import {Router, RouterModule, Routes, CanActivateFn} from '@angular/router';
import {AuthService} from './services/authservice';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';

const sesionRequerida: CanActivateFn = () =>
    inject(AuthService).estaLogueado() || inject(Router).createUrlTree(['/login']);

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent, canActivate: [sesionRequerida]},
    { path: 'admin', component: Admincomponent },
    { path: 'confirm-email', component: Confirmaremailcomponent },
    { path: '**', redirectTo: 'login' },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
