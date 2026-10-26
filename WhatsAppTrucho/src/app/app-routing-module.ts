import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {Paginaprincipalcomponent} from './paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './logincomponent/logincomponent';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
