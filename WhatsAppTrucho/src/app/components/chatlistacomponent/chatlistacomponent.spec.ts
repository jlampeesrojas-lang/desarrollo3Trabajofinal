import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Chat } from '../../models/chat';
import { Chatlistacomponent } from './chatlistacomponent';

describe('Chatlistacomponent', () => {
    let component: Chatlistacomponent;
    let fixture: ComponentFixture<Chatlistacomponent>;
    let el: HTMLElement;

    const chatIa: Chat = {
        id: 1,
        codigo: 'CH-IA0001',
        participantes: [{ id: 1, nombre: 'Tú' }, { id: 100, nombre: 'IA', avatarUrl: 'ia.jpg' }],
        ultimoMensaje: null,
        noLeidos: 0,
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [Chatlistacomponent],
        }).compileComponents();

        fixture = TestBed.createComponent(Chatlistacomponent);
        component = fixture.componentInstance;
        el = fixture.nativeElement as HTMLElement;
        fixture.componentRef.setInput('chats', [chatIa]);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('muestra el chat IA con su foto de perfil y sin mensajes', () => {
        const items = el.querySelectorAll('.item');
        expect(items.length).toBe(1);
        expect(items[0].querySelector('.nombre')?.textContent).toContain('IA');
        expect(items[0].querySelector('img')?.getAttribute('src')).toBe('ia.jpg');
        expect(items[0].querySelector('.previa')?.textContent).toContain('Sin mensajes');
    });

    it('si la foto no carga, muestra las iniciales', () => {
        (el.querySelector('img') as HTMLImageElement).dispatchEvent(new Event('error'));
        fixture.detectChanges();
        expect(el.querySelector('img')).toBeNull();
        expect(el.querySelector('.avatar')?.textContent).toContain('I');
    });

    it('filtra por nombre o por código', () => {
        const caja = el.querySelector('.buscar') as HTMLInputElement;
        caja.value = 'zzz';
        caja.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        expect(el.querySelectorAll('.item').length).toBe(0);
        expect(el.querySelector('.estado')?.textContent).toContain('Ningún chat');

        caja.value = 'ch-ia';
        caja.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        expect(el.querySelectorAll('.item').length).toBe(1);
    });

    it('al hacer clic avisa qué chat se eligió y lo marca como activo', () => {
        let elegido: number | undefined;
        component.seleccionar.subscribe((id: number) => (elegido = id));
        (el.querySelector('.item') as HTMLButtonElement).click();
        expect(elegido).toBe(1);

        fixture.componentRef.setInput('activoId', 1);
        fixture.detectChanges();
        expect(el.querySelector('.item')?.classList.contains('activo')).toBe(true);
    });

    it('muestra la vista previa, la hora y los no leídos cuando hay mensajes', () => {
        const conMensaje: Chat = {
            ...chatIa,
            noLeidos: 2,
            ultimoMensaje: {
                id: 'm-1', chatId: 1, remitenteId: 100, remitenteNombre: 'IA', contenido: 'Hola, soy IA',
                fechaEnvio: new Date().toISOString(), estado: 'ENVIADO',
            },
        };
        fixture.componentRef.setInput('chats', [conMensaje]);
        fixture.detectChanges();
        expect(el.querySelector('.previa')?.textContent).toContain('Hola, soy IA');
        expect(el.querySelector('.badge')?.textContent).toContain('2');
        expect(el.querySelector('.hora')?.textContent?.trim()).not.toBe('');
    });
});
