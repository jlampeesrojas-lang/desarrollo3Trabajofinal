import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { ChatApi, CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE } from '../../servicesChat/chatapi';
import { ChatEventosService } from '../../servicesChat/chateventosservice';
import { ChatMockService } from '../../servicesChat/chatmockservice';
import { Paginachatcomponent } from './paginachatcomponent';

describe('Paginachatcomponent', () => {
  let fixture: ComponentFixture<Paginachatcomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Paginachatcomponent],
      providers: [
        provideRouter([]),
        { provide: CHAT_USUARIO_ID, useValue: 1 },
        { provide: CHAT_USUARIO_NOMBRE, useValue: 'usuario' },
        { provide: ChatApi, useClass: ChatMockService },
        ChatEventosService,
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Paginachatcomponent);
    fixture.detectChanges();
  });

  it('se crea y pide seleccionar un chat si no hay ninguno abierto', () => {
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.componentInstance.hayChat()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Selecciona un chat');
  });
});
