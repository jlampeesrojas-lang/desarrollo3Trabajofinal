import { NgModule, provideBrowserGlobalErrorListeners } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing-module';
import { App } from './app';
import { Navbarcomponent } from './navbarcomponent/navbarcomponent';
import { Footercomponent } from './footercomponent/footercomponent';
import { Paginaprincipalcomponent } from './paginaprincipalcomponent/paginaprincipalcomponent';

@NgModule({
  declarations: [App, Navbarcomponent, Footercomponent, Paginaprincipalcomponent],
  imports: [BrowserModule, AppRoutingModule],
  providers: [provideBrowserGlobalErrorListeners()],
  bootstrap: [App],
})
export class AppModule {}
