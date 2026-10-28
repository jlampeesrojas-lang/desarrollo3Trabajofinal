import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {authGuard} from './guards/authenticationguard';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent, canActivate: [authGuard] },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}