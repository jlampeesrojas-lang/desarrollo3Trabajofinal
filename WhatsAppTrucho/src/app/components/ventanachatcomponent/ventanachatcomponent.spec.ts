import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ChatApi, CHAT_USUARIO_ID, CHAT_USUARIO_NOMBRE } from '../../servicesChat/chatapi';
import { ChatEventosService } from '../../servicesChat/chateventosservice';
import { ChatMockService } from '../../servicesChat/chatmockservice';
import { Ventanachatcomponent } from './ventanachatcomponent';

const esperar = (ms = 400) => vi.advanceTimersByTimeAsync(ms);

/** Prepara el módulo, agrega `extras` mensajes al chat 1 (el de IA) y recién entonces crea el componente. */
async function iniciar(id: string, extras = 0): Promise<ComponentFixture<Ventanachatcomponent>> {
  TestBed.configureTestingModule({
    imports: [Ventanachatcomponent],
    providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
      { provide: CHAT_USUARIO_ID, useValue: 1 },
      { provide: CHAT_USUARIO_NOMBRE, useValue: 'usuario' },
      { provide: ChatApi, useClass: ChatMockService },
      ChatEventosService,
    ],
  });
  const api = TestBed.inject(ChatApi);
  for (let i = 1; i <= extras; i++) {
    api.enviarMensaje(1, `mensaje ${i}`).subscribe();
    await esperar();
  }
  const fixture = TestBed.createComponent(Ventanachatcomponent);
  fixture.detectChanges();
  await esperar();
  fixture.detectChanges();
  return fixture;
}

describe('Ventanachatcomponent', () => {
  let fixture: ComponentFixture<Ventanachatcomponent>;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('muestra el chat de IA vacío, con su foto y su código', async () => {
    fixture = await iniciar('1');
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toMatch(/Código: CH-[A-Z0-9]{6}/);
    expect(el.querySelector('.encabezado img')?.getAttribute('src')).toBe('ia.jpg');
    expect(el.textContent).toContain('Aún no hay mensajes');
    expect(el.querySelectorAll('app-burbujamensajecomponent').length).toBe(0);
  });

  it('historial paginado: 20 mensajes y luego los anteriores (25 en total)', async () => {
    fixture = await iniciar('1', 25);
    expect(fixture.nativeElement.querySelectorAll('app-burbujamensajecomponent').length).toBe(20);
    fixture.componentInstance.cargarAnteriores();
    await esperar();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('app-burbujamensajecomponent').length).toBe(25);
    await esperar(1000);
  });

  it('envía un mensaje: pasa de ENVIANDO a ENVIADO', async () => {
    fixture = await iniciar('1');
    fixture.componentInstance.texto = 'Hola';
    fixture.componentInstance.enviar();
    expect(fixture.componentInstance.mensajes().at(-1)?.estado).toBe('ENVIANDO');
    await esperar();
    expect(fixture.componentInstance.mensajes().at(-1)?.estado).toBe('ENVIADO');
    await esperar(1000);
  });

  it('un mensaje con #error queda en ERROR', async () => {
    fixture = await iniciar('1');
    fixture.componentInstance.texto = 'falla #error';
    fixture.componentInstance.enviar();
    await esperar();
    expect(fixture.componentInstance.mensajes().at(-1)?.estado).toBe('ERROR');
    await esperar(1000);
  });

  it('avisa cuando el chat no existe', async () => {
    fixture = await iniciar('99');
    expect(fixture.nativeElement.textContent).toContain('Este chat no existe');
  });
});
