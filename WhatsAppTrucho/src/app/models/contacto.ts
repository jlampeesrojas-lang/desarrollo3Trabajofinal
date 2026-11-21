/** Contacto tal como lo devuelve el back (GET /contacts/getall). id = id del usuario contacto. */
export interface Contacto {
    id: number;
    usuario: string;
    nombre: string;
    alias: string | null;
}

/** Cuerpo para agregar un contacto (POST /contacts/createjson). usuario = nombre de usuario o correo. */
export interface NuevoContacto {
    usuario: string;
    alias?: string;
}