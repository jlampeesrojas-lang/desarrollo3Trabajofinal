import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Chat } from '../models/chatmodel';
import { CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE } from './chatapi';
import { ChatMockService } from './chatmockservice';

describe('ChatMockService', () => {
  let api: ChatMockService;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        ChatMockService,
        { provide: CHAT_USUARIO_ID, useValue: 1 },
        { provide: CHAT_USUARIO_NOMBRE, useValue: 'usuario' },
      ],
    });
    api = TestBed.inject(ChatMockService);
  });
  afterEach(() => vi.useRealTimers());

  const esperar = () => vi.advanceTimersByTimeAsync(400);

  it('empieza con un único chat: el de IA, vacío y con su foto de perfil', async () => {
    let chats: Chat[] | undefined;
    api.listarChats().subscribe(c => (chats = c));
    await esperar();
    expect(chats?.length).toBe(1);
    expect(chats![0].ultimoMensaje).toBeNull();
    const ia = chats![0].participantes.find(p => p.id !== 1);
    expect(ia?.nombre).toBe('IA');
    expect(ia?.avatarUrl).toBe('ia.jpg');
    expect(chats![0].codigo).toMatch(/^CH-[A-Z0-9]{6}$/);
  });

  it('el chat de IA no tiene mensajes', async () => {
    let total = -1;
    api.listarMensajes(1, 0, 20).subscribe(p => (total = p.items.length));
    await esperar();
    expect(total).toBe(0);
  });

  it('no tiene contactos de ejemplo', async () => {
    let n = -1;
    api.listarContactos().subscribe(c => (n = c.length));
    await esperar();
    expect(n).toBe(0);
  });

  it('crea chats con código único y no duplica el chat con el mismo contacto', async () => {
    api.contactos = [{ id: 2, nombre: 'Ana' }, { id: 3, nombre: 'Beto' }];
    const creados: Chat[] = [];
    api.crearChat(2).subscribe(c => creados.push(c));
    api.crearChat(3).subscribe(c => creados.push(c));
    await esperar();
    api.crearChat(2).subscribe(c => creados.push(c));
    await esperar();
    expect(creados[0].codigo).toMatch(/^CH-[A-Z0-9]{6}$/);
    expect(creados[0].codigo).not.toBe(creados[1].codigo);
    expect(creados[2].id).toBe(creados[0].id);
  });

  it('rechaza un contacto que no existe', async () => {
    let status = 0;
    api.crearChat(99).subscribe({ error: e => (status = e.status) });
    await esperar();
    expect(status).toBe(400);
  });
});
