import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { HttpResponse } from "@angular/common/http";
import { AuthService } from "../../services/authservice";
import { AdminService } from "../../services/adminservice";
import { EditarUsuario, RegistroAuditoria, UsuarioAdmin } from "../../models/admin";

const POR_PAGINA = 10;

const ACCIONES: { [clave: string]: string } = {
    REGISTER: "Cuenta creada",
    EMAIL_CONFIRMED: "Correo confirmado felicidades ya puedes ingresar",
    CONFIRMATION_RESENT: "Reenvío de confirmación",
    LOGIN_OK: "Inicio de sesión",
    LOGIN_FAIL: "Inicio de sesión fallido",
    LOGOUT: "Cierre de sesión",
    TOKEN_REFRESH: "Renovación de sesión",
    TOKEN_REUSE_DETECTED: "Token reutilizado -_-",
    PASSWORD_RESET_REQUESTED: "Recuperar contraseña",
    PASSWORD_RESET_DONE: "Contraseña cambiada",
    MESSAGE_SENT: "Mensaje enviado",
    ADMIN_READ_CHAT: "Chat leído por admin",
    ADMIN_USER_UPDATED: "Usuario modificado por admin",
};

@Component({
    selector: "app-admincomponent",
    standalone: false,
    styleUrl: "./admincomponent.css",
    templateUrl: "./admincomponent.html",
})
export class Admincomponent implements OnInit {
    seccion: "usuarios" | "auditoria" | "chats" = "usuarios";

    usuarios: UsuarioAdmin[] = [];
    cargandoUsuarios = false;
    errorUsuarios = "";
    mensaje = "";
    busqueda = "";
    miUsuario: string | null = null;

    modalAbierto = false;
    modo = "crear";
    formulario = {id: 0, username: "", email: "", password: "", role: "USER"};
    errorModal = "";
    guardando = false;

    usuarioAEliminar: UsuarioAdmin | null = null;
    eliminando = false;

    registros: RegistroAuditoria[] = [];
    cargandoAuditoria = false;
    errorAuditoria = "";
    busquedaAuditoria = "";
    fechaAuditoria = "";
    paginaAuditoria = 0;

    constructor(private auth: AuthService, private admin: AdminService,
                private router: Router, private cdr: ChangeDetectorRef) {
    }

    ngOnInit() {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(["/login"]);
            return;
        }
        if (this.auth.obtenerRol() !== "ADMIN") {
            this.router.navigate(["/principal"]);
            return;
        }
        this.miUsuario = this.auth.obtenerUsuario();
        this.cargarUsuarios();
    }

    irASeccion(seccion: "usuarios" | "auditoria" | "chats") {
        this.seccion = seccion;
        this.mensaje = "";
        if (seccion === "usuarios") {
            this.cargarUsuarios();
        }
        if (seccion === "auditoria") {
            this.cargarAuditoria();
        }
    }

    salir() {
        this.auth.cerrarSesion();
        this.router.navigate(["/login"]);
    }

    mensajeDeError(e: any, mensajePorDefecto: string): string {
        if (e.status === 401) {
            this.auth.cerrarSesion();
            this.router.navigate(["/login"]);
            return "Tu sesión venció, inicia sesión de nuevo";
        }
        if (typeof e.error === "string" && e.error !== "") {
            return e.error;
        }
        if (e.error && e.error.mensaje) {
            return e.error.mensaje;
        }
        if (e.status === 0) {
            return "No se pudo conectar con el servidor";
        }
        if (e.status === 403) {
            return "No tienes permiso para hacer esto";
        }
        return mensajePorDefecto;
    }

    cargarUsuarios() {
        this.cargandoUsuarios = true;
        this.errorUsuarios = "";

        this.admin.listarUsuarios().subscribe({
            next: (lista: UsuarioAdmin[]) => {
                if (lista == null) {
                    this.usuarios = [];
                } else {
                    this.usuarios = lista;
                }
                this.cargandoUsuarios = false;
                this.cdr.markForCheck();
            },
            error: (e: any) => {
                this.cargandoUsuarios = false;
                this.errorUsuarios = this.mensajeDeError(e, "No se pudieron cargar los usuarios");
                this.cdr.markForCheck();
            },
        });
    }

    usuariosFiltrados(): UsuarioAdmin[] {
        const texto = this.busqueda.trim().toLowerCase();
        if (texto === "") {
            return this.usuarios;
        }
        const resultado: UsuarioAdmin[] = [];
        for (const u of this.usuarios) {
            const todo = (u.username + " " + u.email + " " + u.role).toLowerCase();
            if (todo.indexOf(texto) !== -1) {
                resultado.push(u);
            }
        }
        return resultado;
    }

    textoEstado(u: UsuarioAdmin): string {
        if (u.emailVerified === true) {
            return "Verificado";
        }
        if (u.emailVerified === false) {
            return "Pendiente";
        }
        return "—";
    }

    esYo(u: UsuarioAdmin): boolean {
        return u.username === this.miUsuario;
    }

    abrirCrear() {
        this.mensaje = "";
        this.modo = "crear";
        this.formulario = {id: 0, username: "", email: "", password: "", role: "USER"};
        this.errorModal = "";
        this.modalAbierto = true;
    }

    abrirEditar(u: UsuarioAdmin) {
        this.mensaje = "";
        this.modo = "editar";
        this.formulario = {id: u.id, username: u.username, email: u.email, password: "", role: u.role};
        this.errorModal = "";
        this.modalAbierto = true;
    }

    cerrarModal() {
        this.modalAbierto = false;
    }

    validarClave(clave: string): string {
        if (clave.length < 12) {
            return "La contraseña debe tener mínimo 12 caracteres";
        }
        if (!/[A-Z]/.test(clave)) {
            return "La contraseña debe tener al menos una mayúscula";
        }
        if (!/[0-9]/.test(clave)) {
            return "La contraseña debe tener al menos un número";
        }
        if (!/[^A-Za-z0-9]/.test(clave)) {
            return "La contraseña debe tener al menos un símbolo";
        }
        return "";
    }

    guardarUsuario() {
        const f = this.formulario;
        this.errorModal = "";

        if (f.username.trim() === "") {
            this.errorModal = "Escribe el nombre de usuario";
            return;
        }
        if (this.modo === "crear" && f.email.trim() === "") {
            this.errorModal = "Escribe el correo";
            return;
        }
        if (this.modo === "crear" && f.password === "") {
            this.errorModal = "Escribe la contraseña";
            return;
        }
        if (f.password !== "") {
            const errorClave = this.validarClave(f.password);
            if (errorClave !== "") {
                this.errorModal = errorClave;
                return;
            }
        }

        this.guardando = true;

        if (this.modo === "crear") {
            this.admin.crearUsuario({
                username: f.username.trim(),
                email: f.email.trim(),
                password: f.password,
                role: f.role,
            }).subscribe({
                next: () => {
                    this.guardando = false;
                    this.modalAbierto = false;
                    this.mensaje = "Usuario creado correctamente";
                    this.cargarUsuarios();
                    this.cdr.markForCheck();
                },
                error: (e: any) => {
                    this.guardando = false;
                    this.errorModal = this.mensajeDeError(e, "No se pudo crear el usuario");
                    this.cdr.markForCheck();
                },
            });
        } else {
            const cambios: EditarUsuario = {username: f.username.trim(), role: f.role};
            if (f.password !== "") {
                cambios.password = f.password;
            }

            this.admin.actualizarUsuario(f.id, cambios).subscribe({
                next: (resp: HttpResponse<string>) => {
                    this.guardando = false;
                    if (resp.status !== 202) {
                        this.errorModal = resp.body ? resp.body : "El nombre de usuario ya está en uso";
                    } else {
                        this.modalAbierto = false;
                        this.mensaje = "Usuario actualizado correctamente";
                        this.cargarUsuarios();
                    }
                    this.cdr.markForCheck();
                },
                error: (e: any) => {
                    this.guardando = false;
                    this.errorModal = this.mensajeDeError(e, "No se pudo actualizar el usuario");
                    this.cdr.markForCheck();
                },
            });
        }
    }

    pedirEliminar(u: UsuarioAdmin) {
        this.mensaje = "";
        this.usuarioAEliminar = u;
    }

    cancelarEliminar() {
        this.usuarioAEliminar = null;
    }

    confirmarEliminar() {
        const u = this.usuarioAEliminar;
        if (u == null) {
            return;
        }
        this.eliminando = true;
        this.admin.eliminarUsuario(u.id).subscribe({
            next: () => {
                this.eliminando = false;
                this.usuarioAEliminar = null;
                this.mensaje = "Usuario eliminado correctamente";
                this.cargarUsuarios();
                this.cdr.markForCheck();
            },
            error: (e: any) => {
                this.eliminando = false;
                this.usuarioAEliminar = null;
                this.errorUsuarios = this.mensajeDeError(e, "No se pudo eliminar el usuario");
                this.cdr.markForCheck();
            },
        });
    }

    cargarAuditoria() {
        this.cargandoAuditoria = true;
        this.errorAuditoria = "";

        this.admin.listarAuditoria(200).subscribe({
            next: (lista: RegistroAuditoria[]) => {
                if (lista == null) {
                    this.registros = [];
                } else {
                    this.registros = lista;
                }
                this.paginaAuditoria = 0;
                this.cargandoAuditoria = false;
                this.cdr.markForCheck();
            },
            error: (e: any) => {
                this.cargandoAuditoria = false;
                this.errorAuditoria = this.mensajeDeError(e, "No se pudo cargar la auditoría");
                this.cdr.markForCheck();
            },
        });
    }

    nombreAccion(accion: string): string {
        if (ACCIONES[accion]) {
            return ACCIONES[accion];
        }
        return accion;
    }

    resultadoAuditoria(r: RegistroAuditoria): string {
        if (r.action === "LOGIN_FAIL" || r.action === "TOKEN_REUSE_DETECTED") {
            return "Fallido";
        }
        return "Correcto";
    }

    auditoriaFiltrada(): RegistroAuditoria[] {
        const texto = this.busquedaAuditoria.trim().toLowerCase();
        const resultado: RegistroAuditoria[] = [];
        for (const r of this.registros) {
            if (this.fechaAuditoria !== "" && r.createdAt.substring(0, 10) !== this.fechaAuditoria) {
                continue;
            }
            if (texto !== "") {
                const todo = ((r.username ? r.username : "") + " " + this.nombreAccion(r.action) + " " +
                    (r.ip ? r.ip : "") + " " + (r.location ? r.location : "") + " " +
                    (r.detail ? r.detail : "")).toLowerCase();
                if (todo.indexOf(texto) === -1) {
                    continue;
                }
            }
            resultado.push(r);
        }
        return resultado;
    }

    auditoriaPagina(): RegistroAuditoria[] {
        const inicio = this.paginaAuditoria * POR_PAGINA;
        return this.auditoriaFiltrada().slice(inicio, inicio + POR_PAGINA);
    }

    totalPaginasAuditoria(): number {
        const total = Math.ceil(this.auditoriaFiltrada().length / POR_PAGINA);
        if (total < 1) {
            return 1;
        }
        return total;
    }

    paginaAnterior() {
        if (this.paginaAuditoria > 0) {
            this.paginaAuditoria = this.paginaAuditoria - 1;
        }
    }

    paginaSiguiente() {
        if (this.paginaAuditoria < this.totalPaginasAuditoria() - 1) {
            this.paginaAuditoria = this.paginaAuditoria + 1;
        }
    }
}