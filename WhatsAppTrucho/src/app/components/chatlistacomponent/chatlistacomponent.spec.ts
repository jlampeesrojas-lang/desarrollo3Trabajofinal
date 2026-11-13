import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ChatApi, CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE } from '../../services/chatapi';
import { ChatEventosService } from '../../services/chateventosservice';
import { ChatMockService } from '../../services/chatmockservice';
import { Chatlistacomponent } from './chatlistacomponent';

describe('Chatlistacomponent', () => {
  let fixture: ComponentFixture<Chatlistacomponent>;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [Chatlistacomponent],
      providers: [
        provideRouter([]),
        { provide: CHAT_USUARIO_ID, useValue: 1 },
        { provide: CHAT_USUARIO_NOMBRE, useValue: 'usuario' },
        { provide: ChatApi, useClass: ChatMockService },
        ChatEventosService,
      ],
    }).compileComponents();
  });
  afterEach(() => vi.useRealTimers());

  const el = () => fixture.nativeElement as HTMLElement;
  const esperar = async () => {
    await vi.advanceTimersByTimeAsync(400);
    fixture.detectChanges();
  };
  const cargar = async () => {
    fixture = TestBed.createComponent(Chatlistacomponent);
    fixture.detectChanges();
    await esperar();
  };
  const abrirPanel = async () => {
    (el().querySelector('.nuevo') as HTMLButtonElement).click();
    await esperar();
  };

  it('muestra "Cargando" y luego un único chat: IA, vacío y con su foto', async () => {
    fixture = TestBed.createComponent(Chatlistacomponent);
    fixture.detectChanges();
    expect(el().textContent).toContain('Cargando chats');
    await esperar();
    expect(el().querySelectorAll('a.item').length).toBe(1);
    const item = el().querySelector('a.item')!;
    expect(item.textContent).toContain('IA');
    expect(item.textContent).toContain('Sin mensajes todavía');
    expect(item.querySelector('img')?.getAttribute('src')).toBe('ia.jpg');
  });

  it('el panel "Nuevo chat" avisa que aún no hay contactos', async () => {
    await cargar();
    await abrirPanel();
    expect(el().textContent).toContain('Nuevo chat');
    expect(el().textContent).toContain('No tienes contactos todavía');
  });

  it('crear un chat con un contacto lo agrega a la lista y navega a él', async () => {
    (TestBed.inject(ChatApi) as ChatMockService).contactos = [{ id: 2, nombre: 'Ana Gómez' }];
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await cargar();
    await abrirPanel();
    (el().querySelector('button.contacto') as HTMLButtonElement).click();
    await esperar();
    await esperar(); // refresco de la lista
    expect(navegar).toHaveBeenCalledWith(['/chats', 2]);
    expect(el().querySelectorAll('a.item').length).toBe(2);
  });

  it('filtra por nombre o código', async () => {
    await cargar();
    fixture.componentInstance.busqueda.set('zzz');
    fixture.detectChanges();
    expect(el().textContent).toContain('Ningún chat coincide');
    fixture.componentInstance.busqueda.set('ia');
    fixture.detectChanges();
    expect(el().querySelectorAll('a.item').length).toBe(1);
  });
});
