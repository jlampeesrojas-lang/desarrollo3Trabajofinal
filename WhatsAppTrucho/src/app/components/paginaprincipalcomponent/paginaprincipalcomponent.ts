import {Component, OnInit} from '@angular/core';
import {AuthService} from "../../services/authservice";
import {Router} from "@angular/router";

@Component({
    selector: 'app-paginaprincipalcomponent',
    standalone: false,
    styleUrl: './paginaprincipalcomponent.css',
    templateUrl: './paginaprincipalcomponent.html',
})
export class Paginaprincipalcomponent implements OnInit {
    constructor(private auth: AuthService, private router: Router) {
    }

    ngOnInit() {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(['/login']);
        }
    }
}
