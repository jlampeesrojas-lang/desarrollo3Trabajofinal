import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Logincomponent} from './components/logincomponent/logincomponent';
import {Admincomponent} from './components/admincomponent/admincomponent';
import {Confirmaremailcomponent} from './components/confirmaremailcomponent/confirmaremailcomponent';

const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: Logincomponent },

   { path: 'principal', redirectTo: 'chats', pathMatch: 'full' },
    {
        path: 'chats',
        loadChildren: () => import('./chatroutes').then(m => m.CHAT_ROUTES),
    },
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
