import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';
import {authenticationGuard} from './guards/authentication-guard';
import {adminGuard} from './guards/admin-guard';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent, canActivate: [authenticationGuard] },
    { path: 'admin', component: Admincomponent, canActivate: [adminGuard] },
    { path: 'confirm-email', component: Confirmaremailcomponent },
    { path: '**', redirectTo: 'login' },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}