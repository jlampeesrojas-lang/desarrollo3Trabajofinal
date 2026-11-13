import { Provider, inject } from '@angular/core';
import { API, AuthService, USAR_MOCK } from './services/authservice';
import { ChatEventosService } from './services/chateventosservice';
import { CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE, ChatApi } from './services/chatapi';
import { CHAT_API_URL, ChatHttpService } from './services/chathttpservice';
import { ChatMockService } from './services/chatmockservice';

/**
 * Proveedores del módulo de chat.
 * - Interruptor de mocks: el MISMO de Front 1 (USAR_MOCK en services/authservice.ts).
 * - Sesión: el id sale del claim "uid" del JWT (lo pone Back 1) y el nombre del claim "sub".
 *   Con el login falso (mocks) el token no trae "uid", así que se usa 1.
 */
export function proveedoresChat(): Provider[] {
  return [
    {
      provide: CHAT_USUARIO_ID,
      useFactory: () => Number(inject(AuthService).leerDatosDelToken()?.uid ?? 1),
    },
    {
      provide: CHAT_USUARIO_NOMBRE,
      useFactory: () => inject(AuthService).obtenerUsuario() ?? 'Tú',
    },
    { provide: CHAT_API_URL, useValue: API },
    { provide: ChatApi, useClass: USAR_MOCK ? ChatMockService : ChatHttpService },
    ChatEventosService,
  ];
}
