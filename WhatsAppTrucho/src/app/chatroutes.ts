import { Routes } from '@angular/router';
import { proveedoresChat } from './chatproviders';
import { Paginachatcomponent } from './components/paginachatcomponent/paginachatcomponent';
import { Ventanachatcomponent } from './components/ventanachatcomponent/ventanachatcomponent';

export const CHAT_ROUTES: Routes = [
  {
    path: '',
    component: Paginachatcomponent,
    providers: proveedoresChat(),
    children: [{ path: ':id', component: Ventanachatcomponent }],
  },
];
