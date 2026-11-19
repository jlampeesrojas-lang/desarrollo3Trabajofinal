import {Component} from "@angular/core";
import {Router} from "@angular/router";
import {AuthService} from "../../services/authservice";

/** Pantalla de error: 401 sesión vencida, 403 sin permiso y 404 página no encontrada. El código viene en la ruta (/error/401). */
@Component({
    selector: "app-errorcomponent",
    standalone: false,
    styleUrl: "./errorcomponent.css",
    templateUrl: "./errorcomponent.html",
})
export class Errorcomponent {

    constructor(private auth: AuthService, private router: Router) {
    }

    /** Código del error, tomado de la dirección (por ejemplo /error/401). */
    get codigo(): string {
        return this.router.url.replace("/error/", "");
    }

    /** Mensaje que se le muestra al usuario según el código. */
    get mensaje(): string {
        if (this.codigo === "401") {
            return "Tu sesión venció. Inicia sesión de nuevo para continuar.";
        }
        if (this.codigo === "403") {
            return "No tienes permiso para hacer esto.";
        }
        return "No encontramos lo que buscas.";
    }

    /** Texto del botón según el código. */
    get textoBoton(): string {
        if (this.codigo === "401") {
            return "Iniciar sesión";
        }
        if (this.codigo === "403") {
            return "Entendido";
        }
        return "Volver al inicio";
    }

    /** 401 cierra la sesión y va al login. 403 vuelve a la pantalla del admin o a la principal. 404 va al inicio. */
    pulsar() {
        if (this.codigo === "401") {
            this.auth.cerrarSesion();
            this.router.navigate(["/login"]);
            return;
        }
        if (this.codigo === "403" && this.auth.obtenerRol() === "ADMIN") {
            this.router.navigate(["/admin"]);
            return;
        }
        this.router.navigate(["/principal"]);
    }
}