/** Campos del token JWT que usa el front. */
interface DatosToken {
    exp?: number;
    sub?: string;
    role?: string;
}