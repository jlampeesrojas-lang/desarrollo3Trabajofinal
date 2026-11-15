import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Paginachatcomponent } from './paginachatcomponent';
import { Chatlistacomponent } from '../chatlistacomponent/chatlistacomponent';
import { Ventanachatcomponent } from '../ventanachatcomponent/ventanachatcomponent';
import { Burbujamensajecomponent } from '../burbujamensajecomponent/burbujamensajecomponent';

describe('Paginachatcomponent', () => {
    let component: Paginachatcomponent;
    let fixture: ComponentFixture<Paginachatcomponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [Paginachatcomponent, Chatlistacomponent, Ventanachatcomponent, Burbujamensajecomponent],
        }).compileComponents();

        fixture = TestBed.createComponent(Paginachatcomponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('empieza con un solo chat (IA) y sin chat activo', () => {
        expect(component.chats.length).toBe(1);
        expect(component.chats[0].participantes[1].nombre).toBe('IA');
        expect(component.chatActivo).toBeNull();
    });

    it('al enviar, el mensaje pasa de ENVIANDO a ENVIADO (sin respuesta automática)', () => {
        vi.useFakeTimers();
        component.seleccionarChat(1);
        component.enviar('hola');
        expect(component.mensajesActivos[0].estado).toBe('ENVIANDO');
        vi.advanceTimersByTime(600);
        expect(component.mensajesActivos[0].estado).toBe('ENVIADO');
        vi.advanceTimersByTime(5000);
        expect(component.mensajesActivos.length).toBe(1);
    });

    it('#error deja el mensaje en ERROR y reintentar lo envía', () => {
        vi.useFakeTimers();
        component.seleccionarChat(1);
        component.enviar('prueba #error');
        vi.advanceTimersByTime(600);
        const fallido = component.mensajesActivos[0];
        expect(fallido.estado).toBe('ERROR');
        component.reintentar({ ...fallido, contenido: 'prueba' });
        vi.advanceTimersByTime(600);
        expect(component.mensajesActivos[0].estado).toBe('ENVIADO');
    });

    it('sin conexión el envío falla y al reconectar vuelve a CONECTADO', () => {
        vi.useFakeTimers();
        component.seleccionarChat(1);
        component.simularCaida();
        expect(component.conexion).toBe('RECONECTANDO');
        component.enviar('hola');
        vi.advanceTimersByTime(600);
        expect(component.mensajesActivos[0].estado).toBe('ERROR');
        vi.advanceTimersByTime(4000);
        expect(component.conexion).toBe('CONECTADO');
        expect(component.conexionRestablecida).toBe(true);
    });

});
