import { Component } from "@angular/core";
import { Router } from '@angular/router';

@Component({
  selector: "app-logincomponent",
  standalone: false,
  styleUrl: "./logincomponent.css",
  templateUrl: "./logincomponent.html",
})
export class Logincomponent {
    vista: 'login' | 'registro' | 'verificacion' | 'recuperar' | 'codigoRecuperar' | 'nuevaClave' = 'login';
    constructor(private router: Router) {}

    iniciarSesion() {
        this.router.navigate(['/principal']);
    }
}
