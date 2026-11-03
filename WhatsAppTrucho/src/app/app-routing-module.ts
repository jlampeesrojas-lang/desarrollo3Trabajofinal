import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';
import {authGuard} from './guards/authenticationguard';
import {adminGuard} from './guards/adminguard';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },
    { path: 'principal', redirectTo: 'chats', pathMatch: 'full' },
    {
        path: 'chats',
        canActivate: [authGuard],
        loadChildren: () => import('./chatroutes').then(m => m.CHAT_ROUTES),
    },
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
