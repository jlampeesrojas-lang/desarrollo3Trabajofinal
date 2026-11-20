import { Component, signal } from '@angular/core';
import {AuthService} from "./services/authservice";
import {Router} from "@angular/router";

@Component({
  selector: 'app-root',
  standalone: false,
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
    constructor(public auth: AuthService, private router: Router) {}

    irAlLogin() {
        this.auth.cerrarSesion();
        this.router.navigate(['/login']);
    }

    protected readonly title = signal('untitled2');
}

