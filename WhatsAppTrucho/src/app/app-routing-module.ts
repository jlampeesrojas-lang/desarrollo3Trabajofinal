import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Paginaprincipalcomponent} from './components/paginaprincipalcomponent/paginaprincipalcomponent';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';
import {Paginachatcomponent} from './components/paginachatcomponent/paginachatcomponent';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', component: Paginaprincipalcomponent},
    { path: 'admin', component: Admincomponent },
    { path: 'confirm-email', component: Confirmaremailcomponent },
    { path: 'chats', component: Paginachatcomponent },
    { path: '**', redirectTo: 'login' },
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}