import { NgModule, provideBrowserGlobalErrorListeners } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { AppRoutingModule } from "./app-routing-module";
import { App } from "./app";
import { Navbarcomponent } from "./components/navbarcomponent/navbarcomponent";
import { Footercomponent } from "./components/footercomponent/footercomponent";
import { Paginaprincipalcomponent } from "./components/paginaprincipalcomponent/paginaprincipalcomponent";
import { Logincomponent } from "./components/logincomponent/logincomponent";
import { authenticationInterceptor } from "./interceptors/authentication-interceptor";
import { Admincomponent } from "./components/admincomponent/admincomponent";
import { Confirmaremailcomponent } from "./components/confirmaremailcomponent/confirmaremailcomponent";

@NgModule({
    declarations: [
        App,
        Navbarcomponent,
        Footercomponent,
        Paginaprincipalcomponent,
        Logincomponent,
        Admincomponent,
        Confirmaremailcomponent,
    ],
    imports: [BrowserModule, FormsModule, AppRoutingModule],
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideHttpClient(withInterceptors([authenticationInterceptor])),
    ],
    bootstrap: [App],
})
export class AppModule {}