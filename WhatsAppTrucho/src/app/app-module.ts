import { NgModule, provideBrowserGlobalErrorListeners } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { provideHttpClient } from "@angular/common/http";
import { AppRoutingModule } from "./app-routing-module";
import { App } from "./app";
import { Paginaprincipalcomponent } from "./components/paginaprincipalcomponent/paginaprincipalcomponent";
import { Logincomponent } from "./components/logincomponent/logincomponent";
import { Admincomponent } from "./components/admincomponent/admincomponent";
import { Confirmaremailcomponent } from "./components/confirmaremailcomponent/confirmaremailcomponent";
import { Adminchatscomponent } from "./components/adminchatscomponent/adminchatscomponent";
import { Chatscomponent } from "./components/chatscomponent/chatscomponent";
import { Contactoscomponent } from "./components/contactoscomponent/contactoscomponent";
import { Errorcomponent } from "./components/errorcomponent/errorcomponent";
import { Audiocomponent } from "./components/audiocomponent/audiocomponent";
import { Informacioncomponent } from "./components/informacioncomponent/informacioncomponent";

/** Módulo raíz de la aplicación: declara los componentes y registra los módulos que se usan. */
@NgModule({
  declarations: [
    App,
    Paginaprincipalcomponent,
    Logincomponent,
    Admincomponent,
    Confirmaremailcomponent,
    Adminchatscomponent,
    Chatscomponent,
    Contactoscomponent,
    Errorcomponent,
    Audiocomponent,
    Informacioncomponent,
  ],
  imports: [BrowserModule, FormsModule, AppRoutingModule],
  providers: [provideBrowserGlobalErrorListeners(), provideHttpClient()],
  bootstrap: [App],
})
export class AppModule {}