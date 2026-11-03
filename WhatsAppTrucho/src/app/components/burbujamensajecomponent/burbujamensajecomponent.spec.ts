import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Mensaje } from '../../modelsChat/mensajemodel';
import { Burbujamensajecomponent } from './burbujamensajecomponent';

const base: Mensaje = {
  id: 1, chatId: 1, remitenteId: 1, remitenteNombre: 'Luis', contenido: 'Hola',
  tipo: 'TEXTO', fechaEnvio: new Date().toISOString(), estado: 'ENVIADO',
};

describe('Burbujamensajecomponent', () => {
  let fixture: ComponentFixture<Burbujamensajecomponent>;

  function crear(m: Mensaje, esMio: boolean) {
    fixture.componentRef.setInput('mensaje', m);
    fixture.componentRef.setInput('esMio', esMio);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Burbujamensajecomponent] }).compileComponents();
    fixture = TestBed.createComponent(Burbujamensajecomponent);
  });

  it('muestra el contenido y el autor cuando el mensaje es ajeno', () => {
    crear({ ...base, remitenteNombre: 'Valentina' }, false);
    const t = fixture.nativeElement.textContent;
    expect(t).toContain('Hola');
    expect(t).toContain('Valentina');
  });

  it('muestra "Enviando…" mientras se envía', () => {
    crear({ ...base, estado: 'ENVIANDO' }, true);
    expect(fixture.nativeElement.textContent).toContain('Enviando…');
  });

  it('en error muestra "Reintentar" y emite el mensaje', () => {
    const m = { ...base, estado: 'ERROR' as const };
    crear(m, true);
    let emitido: Mensaje | undefined;
    fixture.componentInstance.reintentar.subscribe((x: Mensaje) => (emitido = x));
    fixture.nativeElement.querySelector('.reintentar').click();
    expect(emitido).toEqual(m);
  });
});
