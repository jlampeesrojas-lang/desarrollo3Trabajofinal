import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { Paginaprincipalcomponent } from './paginaprincipalcomponent';
import { Chatscomponent } from '../chatscomponent/chatscomponent';
import { FormsModule } from '@angular/forms';
import {Chatsservice} from "../../services/chatsservice";
import { AuthService } from '../../services/authservice';
import { Router } from '@angular/router';

describe('Paginaprincipalcomponent', () => {
    let component: Paginaprincipalcomponent;
    let fixture: ComponentFixture<Paginaprincipalcomponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({

            declarations: [Paginaprincipalcomponent, Chatscomponent],
            imports: [FormsModule],
            providers: [provideHttpClient(), provideRouter([]),
                { provide: Chatsservice, useValue: { cargar: () => Promise.resolve([]) } }],
        }).compileComponents();

        fixture = TestBed.createComponent(Paginaprincipalcomponent);
        component = fixture.componentInstance;
        await fixture.whenStable();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('cierra la sesión y vuelve al login', () => {
        const cerrar = vi.spyOn(TestBed.inject(AuthService), 'cerrarSesion');
        const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
        localStorage.setItem('token', 'token-de-prueba');
        component.salir();
        expect(cerrar).toHaveBeenCalled();
        expect(localStorage.getItem('token')).toBeNull();
        expect(navegar).toHaveBeenCalledWith(['/login']);
    });
});
