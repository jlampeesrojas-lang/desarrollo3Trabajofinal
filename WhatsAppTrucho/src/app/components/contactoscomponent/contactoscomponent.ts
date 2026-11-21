import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {HttpErrorResponse} from "@angular/common/http";
import {AuthService} from "../../services/authservice";
import {ContactService} from "../../services/contactservice";
import {Contacto} from "../../models/contacto";

/** Pantalla donde el usuario ve, agrega y elimina sus contactos. */
@Component({
    selector: "app-contactoscomponent",
    standalone: false,
    styleUrl: "./contactoscomponent.css",
    templateUrl: "./contactoscomponent.html",
})
/** Pantalla de contactos: lista, busca, agrega y elimina contactos. */
export class Contactoscomponent implements OnInit {
    contactos: Contacto[] = [];
    cargando = false;
    error = "";
    mensaje = "";
    busqueda = "";
    modalAbierto = false;
    formulario = {usuario: "", alias: ""};
    errorModal = "";
    guardando = false;
    contactoAEliminar: Contacto | null = null;
    eliminando = false;

    constructor(private auth: AuthService, private servicio: ContactService,
                private router: Router, private cdr: ChangeDetectorRef) {
    }

    /** Si no hay sesión va al login; si hay, carga los contactos. */
    ngOnInit() {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(["/error", "401"]);
            return;
        }
        this.cargarContactos();
    }

    /** Regresa a la pantalla de chats, tanto para usuarios como para administradores. */
    volver() {
        this.router.navigate(["/principal"]);
    }

    /** Cierra la sesión y va al login. */
    salir() {
        this.auth.cerrarSesion();
        this.router.navigate(["/login"]);
    }

    /** Convierte un error HTTP en un texto para el usuario. 401 cierra la sesión y 403 avisa que no hay permiso. */
    mensajeDeError(fallo: HttpErrorResponse, mensajePorDefecto: string): string {
        if (fallo.status === 401) {
            this.router.navigate(["/error", "401"]);
            return "Tu sesión venció, inicia sesión de nuevo";
        }
        if (fallo.status === 403) {
            this.router.navigate(["/error", "403"]);
            return "No tienes permiso para hacer esto";
        }
        if (typeof fallo.error === "string" && fallo.error !== "") {
            return fallo.error;
        }
        if (fallo.error?.mensaje) {
            return fallo.error.mensaje;
        }
        if (fallo.status === 0) {
            return "No se pudo conectar con el servidor";
        }
        return mensajePorDefecto;
    }

    /** Pide al back la lista de contactos. */
    cargarContactos() {
        this.cargando = true;
        this.error = "";

        this.servicio.listarContactos().subscribe({
            next: (lista: Contacto[]) => {
                if (lista == null) {
                    this.contactos = [];
                } else {
                    this.contactos = lista;
                }
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.cargando = false;
                this.error = this.mensajeDeError(fallo, "No se pudieron cargar los contactos");
                this.cdr.markForCheck();
            },
        });
    }

    /** Contactos que coinciden con lo escrito en el buscador (por nombre o usuario). */
    contactosFiltrados(): Contacto[] {
        const texto = this.busqueda.trim().toLowerCase();
        if (texto === "") {
            return this.contactos;
        }
        const resultado: Contacto[] = [];
        for (const contacto of this.contactos) {
            const todo = `${contacto.nombre} ${contacto.usuario}`.toLowerCase();
            if (todo.indexOf(texto) !== -1) {
                resultado.push(contacto);
            }
        }
        return resultado;
    }

    /** Abre la ventana para agregar un contacto. */
    abrirAgregar() {
        this.mensaje = "";
        this.formulario = {usuario: "", alias: ""};
        this.errorModal = "";
        this.modalAbierto = true;
    }

    /** Cierra la ventana de agregar. */
    cerrarModal() {
        this.modalAbierto = false;
    }

    /** Valida el formulario y agrega el contacto. */
    guardarContacto() {
        const datos = this.formulario;
        this.errorModal = "";

        if (datos.usuario.trim() === "") {
            this.errorModal = "Escribe el nombre de usuario o el correo";
            return;
        }
        if (datos.usuario.indexOf("<") !== -1 || datos.usuario.indexOf(">") !== -1) {
            this.errorModal = "El usuario tiene caracteres no permitidos";
            return;
        }

        this.guardando = true;
        this.servicio.agregarContacto({usuario: datos.usuario.trim(), alias: datos.alias.trim()}).subscribe({
            next: () => {
                this.guardando = false;
                this.modalAbierto = false;
                this.mensaje = "Contacto agregado correctamente";
                this.cargarContactos();
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.guardando = false;
                this.errorModal = this.mensajeDeError(fallo, "No se pudo agregar el contacto");
                this.cdr.markForCheck();
            },
        });
    }

    /** Abre la confirmación para eliminar un contacto. */
    pedirEliminar(contacto: Contacto) {
        this.mensaje = "";
        this.contactoAEliminar = contacto;
    }

    /** Cierra la confirmación de eliminar. */
    cancelarEliminar() {
        this.contactoAEliminar = null;
    }

    /** Elimina el contacto seleccionado. */
    confirmarEliminar() {
        const contacto = this.contactoAEliminar;
        if (contacto == null) {
            return;
        }
        this.eliminando = true;
        this.servicio.eliminarContacto(contacto.id).subscribe({
            next: () => {
                this.eliminando = false;
                this.contactoAEliminar = null;
                this.mensaje = "Contacto eliminado correctamente";
                this.cargarContactos();
                this.cdr.markForCheck();
            },
            error: (fallo: HttpErrorResponse) => {
                this.eliminando = false;
                this.contactoAEliminar = null;
                this.error = this.mensajeDeError(fallo, "No se pudo eliminar el contacto");
                this.cdr.markForCheck();
            },
        });
    }
}