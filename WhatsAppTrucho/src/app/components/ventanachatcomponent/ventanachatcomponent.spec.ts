import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Chat, Mensaje } from '../../models/chat';
import { Ventanachatcomponent } from './ventanachatcomponent';
import { Burbujamensajecomponent } from '../burbujamensajecomponent/burbujamensajecomponent';

describe('Ventanachatcomponent', () => {
    let component: Ventanachatcomponent;
    let fixture: ComponentFixture<Ventanachatcomponent>;
    let el: HTMLElement;

    const chat: Chat = {
        id: 1,
        codigo: 'CH-IA0001',
        participantes: [{ id: 1, nombre: 'Tú' }, { id: 100, nombre: 'IA' }],
        ultimoMensaje: null,
        noLeidos: 0,
    };

    function crear(n: number): Mensaje[] {
        return Array.from({ length: n }, (_, i) => ({
            id: `m-${i}`, chatId: 1, remitenteId: 1, remitenteNombre: 'Tú',
            contenido: `msg ${i}`, fechaEnvio: new Date().toISOString(), estado: 'ENVIADO' as const,
        }));
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [Ventanachatcomponent, Burbujamensajecomponent],
        }).compileComponents();

        fixture = TestBed.createComponent(Ventanachatcomponent);
        component = fixture.componentInstance;
        el = fixture.nativeElement as HTMLElement;
        fixture.componentRef.setInput('chat', chat);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('muestra nombre y código del chat', () => {
        expect(el.textContent).toContain('IA');
        expect(el.textContent).toContain('CH-IA0001');
    });

    it('muestra aviso cuando no hay mensajes', () => {
        expect(el.textContent).toContain('Aún no hay mensajes');
    });

    it('solo muestra la última página y permite cargar anteriores', () => {
        vi.useFakeTimers();
        fixture.componentRef.setInput('mensajes', crear(45));
        fixture.detectChanges();
        expect(el.querySelectorAll('app-burbujamensajecomponent').length).toBe(20);
        component.cargarAnteriores();
        vi.advanceTimersByTime(400);
        fixture.detectChanges();
        expect(el.querySelectorAll('app-burbujamensajecomponent').length).toBe(40);
        vi.useRealTimers();
    });

    it('emite enviar con el texto y limpia la caja', () => {
        const textos: string[] = [];
        component.enviar.subscribe((t: string) => textos.push(t));
        const caja = el.querySelector('textarea') as HTMLTextAreaElement;
        caja.value = '  hola  ';
        component.enviarTexto();
        expect(textos).toEqual(['hola']);
        expect(caja.value).toBe('');
    });

    it('no emite si el texto está vacío', () => {
        let llamadas = 0;
        component.enviar.subscribe(() => llamadas++);
        component.enviarTexto();
        expect(llamadas).toBe(0);
    });

    it('muestra banner de reconexión', () => {
        fixture.componentRef.setInput('conexion', 'RECONECTANDO');
        fixture.detectChanges();
        expect(el.textContent).toContain('Reconectando');
    });
});
