import {ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {AuthService} from "../../services/authservice";
import {AdminService} from "../../services/adminservice";
import {EditarUsuario, RegistroAuditoria, UsuarioAdmin} from "../../models/admin";

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
    MESSAGE_DELETED: "Mensaje eliminado",
    ADMIN_READ_CHAT: "Chat leído por admin",
    ADMIN_USER_UPDATED: "Usuario modificado por admin",
};

/** Panel de administración: usuarios, auditoría y chats. */
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
    usuarioInfo: UsuarioAdmin | null = null;

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
    accionAuditoria = "";
    acciones = Object.keys(ACCIONES);

    registroUbicacion: RegistroAuditoria | null = null;
    resultadoIp = "";

    constructor(private auth: AuthService, private admin: AdminService,
                private router: Router, private cdr: ChangeDetectorRef) {
    }

    /** Se ejecuta al iniciar el componente y carga los datos iniciales. */
    ngOnInit() {
        if (!this.auth.estaLogueado()) {
            this.router.navigate(["/error", "401"]);
            return;
        }
        if (this.auth.obtenerRol() !== "ADMIN") {
            this.router.navigate(["/error", "403"]);
            return;
        }
        this.miUsuario = this.auth.obtenerUsuario();
        this.cargarUsuarios();
    }

    /** Cambia de sección del panel y carga sus datos. */
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

    /** Abre la página de chats para que el administrador vea sus propias conversaciones. */
    irAChats() {
        this.router.navigate(["/principal"]);
    }

    /** Cierra la sesión y vuelve al login. */
    salir() {
        this.auth.cerrarSesion();
        this.router.navigate(["/login"]);
    }

    /** Convierte un error HTTP en un mensaje legible; si no hay uno, usa el mensaje por defecto. */
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

    /** Pide al back la lista de usuarios y la muestra. */
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
            error: (fallo: HttpErrorResponse) => {
                this.cargandoUsuarios = false;
                this.errorUsuarios = this.mensajeDeError(fallo, "No se pudieron cargar los usuarios");
                this.cdr.markForCheck();
            },
        });
    }

    /** Devuelve los usuarios que coinciden con la búsqueda escrita. */
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

    /** Texto del estado de un usuario (confirmado o pendiente). */
    textoEstado(u: UsuarioAdmin): string {
        if (u.emailVerified === true) {
            return "Verificado";
        }
        if (u.emailVerified === false) {
            return "Pendiente";
        }
        return "—";
    }

    /** Dice si el usuario de la fila es el administrador que tiene la sesión abierta. */
    esElMismo(u: UsuarioAdmin): boolean {
        return u.username === this.miUsuario;
    }

    /** Abre la mini ventana con la información de un usuario de la lista. */
    verUsuario(nombre: string) {
        this.usuarioInfo = null;
        for (const u of this.usuarios) {
            if (u.username === nombre) {
                this.usuarioInfo = u;
            }
        }
    }

    /** Cierra la mini ventana de información. */
    cerrarInfo() {
        this.usuarioInfo = null;
    }

    /** Abre la ventana para crear un usuario nuevo. */
    abrirCrear() {
        this.mensaje = "";
        this.modo = "crear";
        this.formulario = {id: 0, username: "", email: "", password: "", role: "USER"};
        this.errorModal = "";
        this.modalAbierto = true;
    }

    /** Abre la ventana para editar un usuario existente. */
    abrirEditar(u: UsuarioAdmin) {
        this.mensaje = "";
        this.modo = "editar";
        this.formulario = {id: u.id, username: u.username, email: u.email, password: "", role: u.role};
        this.errorModal = "";
        this.modalAbierto = true;
    }

    /** Título de la ventana de usuario según sea crear o editar. */
    get tituloModal(): string {
        if (this.modo === "crear") {
            return "Nuevo usuario";
        }
        return "Editar usuario";
    }

    /** Texto de ayuda del campo de contraseña según sea crear o editar. */
    get placeholderClave(): string {
        if (this.modo === "crear") {
            return "Contraseña";
        }
        return "Dejar vacío para no cambiarla";
    }

    /** Cierra la ventana de crear o editar usuario. */
    cerrarModal() {
        this.modalAbierto = false;
    }

    /** Revisa que la contraseña cumpla la política y devuelve el error, o vacío si es válida. */
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

    /** Valida el formulario y crea o actualiza el usuario en el back. */
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
                error: (fallo: HttpErrorResponse) => {
                    this.guardando = false;
                    this.errorModal = this.mensajeDeError(fallo, "No se pudo crear el usuario");
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
                error: (fallo: HttpErrorResponse) => {
                    this.guardando = false;
                    this.errorModal = this.mensajeDeError(fallo, "No se pudo actualizar el usuario");
                    this.cdr.markForCheck();
                },
            });
        }
    }

    /** Pide confirmación antes de eliminar un usuario. */
    pedirEliminar(u: UsuarioAdmin) {
        this.mensaje = "";
        this.usuarioAEliminar = u;
    }

    /** Cancela la eliminación pendiente. */
    cancelarEliminar() {
        this.usuarioAEliminar = null;
    }

    /** Elimina en el back el usuario que se confirmó. */
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
            error: (fallo: HttpErrorResponse) => {
                this.eliminando = false;
                this.usuarioAEliminar = null;
                this.errorUsuarios = this.mensajeDeError(fallo, "No se pudo eliminar el usuario");
                this.cdr.markForCheck();
            },
        });
    }

    /** Muestra la ubicación guardada del registro o la consulta mediante su IP. */
    abrirUbicacion(registro: RegistroAuditoria) {
        this.registroUbicacion = registro;

        if (registro.location) {
            this.resultadoIp = `La acción fue realizada desde ${registro.location}.`;
            return;
        }

        if (!registro.ip) {
            this.resultadoIp = "Este registro no tiene una IP para consultar.";
            return;
        }

        this.resultadoIp = "Consultando la ubicación de la IP…";

        this.admin.ubicarIp(registro.ip).subscribe({
            next: ubicacion => {
                if (this.registroUbicacion !== registro) return;

                this.resultadoIp = `Ubicación aproximada de la IP: ${ubicacion.text}.`;
                this.cdr.markForCheck();
            },
            error: fallo => {
                if (this.registroUbicacion !== registro) return;

                this.resultadoIp = this.mensajeDeError(fallo, "No se pudo ubicar esta IP.");
                this.cdr.markForCheck();
            }
        });
    }

    /** Pide al back los registros de auditoría y los muestra. */
    cargarAuditoria() {
        this.cargandoAuditoria = true;
        this.errorAuditoria = "";

        this.admin.listarAuditoria(200, this.fechaAuditoria).subscribe({
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
            error: (fallo: HttpErrorResponse) => {
                this.cargandoAuditoria = false;
                this.errorAuditoria = this.mensajeDeError(fallo, "No se pudo cargar la auditoría");
                this.cdr.markForCheck();
            },
        });
    }

    /** Nombre legible de una acción de auditoría. */
    nombreAccion(accion: string): string {
        if (ACCIONES[accion]) {
            return ACCIONES[accion];
        }
        return accion;
    }

    /** Resultado (correcto o fallido) de un registro de auditoría. */
    resultadoAuditoria(r: RegistroAuditoria): string {
        if (r.action === "LOGIN_FAIL" || r.action === "TOKEN_REUSE_DETECTED") {
            return "Fallido";
        }
        return "Correcto";
    }

    /** Registros de auditoría que cumplen los filtros elegidos. */
    auditoriaFiltrada(): RegistroAuditoria[] {
        const texto = this.busquedaAuditoria.trim().toLowerCase();
        const resultado: RegistroAuditoria[] = [];
        for (const r of this.registros) {
            if (this.fechaAuditoria !== "" && r.createdAt.substring(0, 10) !== this.fechaAuditoria) {
                continue;
            }
            if (this.accionAuditoria !== "" && r.action !== this.accionAuditoria) {
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

    /** Registros de auditoría de la página actual. */
    auditoriaPagina(): RegistroAuditoria[] {
        const inicio = this.paginaAuditoria * POR_PAGINA;
        return this.auditoriaFiltrada().slice(inicio, inicio + POR_PAGINA);
    }

    /** Cantidad de páginas de la auditoría filtrada. */
    totalPaginasAuditoria(): number {
        const total = Math.ceil(this.auditoriaFiltrada().length / POR_PAGINA);
        if (total < 1) {
            return 1;
        }
        return total;
    }

    /** Pasa a la página anterior de la auditoría. */
    paginaAnterior() {
        if (this.paginaAuditoria > 0) {
            this.paginaAuditoria = this.paginaAuditoria - 1;
        }
    }

    /** Pasa a la página siguiente de la auditoría. */
    paginaSiguiente() {
        if (this.paginaAuditoria < this.totalPaginasAuditoria() - 1) {
            this.paginaAuditoria = this.paginaAuditoria + 1;
        }
    }
}