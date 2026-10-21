import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { Paginaprincipalcomponent } from './paginaprincipalcomponent/paginaprincipalcomponent';


const routes: Routes = [
  { path: '', component: Paginaprincipalcomponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
