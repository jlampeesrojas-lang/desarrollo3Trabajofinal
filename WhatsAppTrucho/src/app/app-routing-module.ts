import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {authGuard} from './guards/authenticationguard';
import {adminGuard} from './guards/adminguard';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent, canActivate: [authGuard] },
    { path: 'admin', component: Admincomponent, canActivate: [adminGuard] },
    { path: '**', redirectTo: 'login' },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}