import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Mensaje } from '../../models/chat';
import { Burbujamensajecomponent } from './burbujamensajecomponent';

describe('Burbujamensajecomponent', () => {
    let component: Burbujamensajecomponent;
    let fixture: ComponentFixture<Burbujamensajecomponent>;

    const base: Mensaje = {
        id: 'm-1',
        chatId: 1,
        remitenteId: 1,
        remitenteNombre: 'Tú',
        contenido: 'Hola',
        fechaEnvio: new Date().toISOString(),
        estado: 'ENVIADO',
    };

    function mostrar(mensaje: Mensaje, esMio: boolean): HTMLElement {
        fixture.componentRef.setInput('mensaje', mensaje);
        fixture.componentRef.setInput('esMio', esMio);
        fixture.detectChanges();
        return fixture.nativeElement as HTMLElement;
    }

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [Burbujamensajecomponent],
        }).compileComponents();

        fixture = TestBed.createComponent(Burbujamensajecomponent);
        component = fixture.componentInstance;
    });

    it('should create', () => {
        mostrar(base, true);
        expect(component).toBeTruthy();
    });

    it('muestra el texto y el visto cuando el mensaje es mío y fue enviado', () => {
        const el = mostrar(base, true);
        expect(el.textContent).toContain('Hola');
        expect(el.querySelector('.estado')?.textContent).toContain('✓');
        expect(el.querySelector('.autor')).toBeNull();
    });

    it('muestra el nombre del autor en los mensajes recibidos', () => {
        const el = mostrar({ ...base, remitenteId: 100, remitenteNombre: 'IA' }, false);
        expect(el.querySelector('.autor')?.textContent).toContain('IA');
        expect(el.querySelector('.estado')).toBeNull();
    });

    it('muestra "Enviando…" mientras se envía', () => {
        const el = mostrar({ ...base, estado: 'ENVIANDO' }, true);
        expect(el.querySelector('.estado')?.textContent).toContain('Enviando');
    });

    it('en error muestra "Reintentar" y avisa al hacer clic', () => {
        const el = mostrar({ ...base, estado: 'ERROR' }, true);
        let emitido: Mensaje | undefined;
        component.reintentar.subscribe((m: Mensaje) => (emitido = m));
        (el.querySelector('.reintentar') as HTMLButtonElement).click();
        expect(emitido?.id).toBe('m-1');
    });
});
