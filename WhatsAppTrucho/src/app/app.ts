import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './services/authservice';

interface Mensaje {
  texto: string;
  hora: string;
  enviado: boolean;
}

interface Chat {
  id: number;
  nombre: string;
  avatar: string;
  imagen: string;
  mensajes: Mensaje[];
}

@Component({
  selector: 'app-root',
  standalone: false,
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  seccion: 'chats' | 'ia' | 'contactos' = 'chats';

  miUsuario = 'Usuario';
  busquedaChat = '';
  mensajeNuevo = '';
  chatSeleccionado = 1;

  chats: Chat[] = [
    {
      id: 1,
      nombre: 'Camila',
      avatar: 'C',
      imagen: '',
      mensajes: [
        {
          texto: '¡Hola! ¿Cómo estás?',
          hora: '10:28',
          enviado: false
        },
        {
          texto: '¡Muy bien! ¿Y tú?',
          hora: '10:29',
          enviado: true
        },
        {
          texto: 'Todo bien, ¿qué haces?',
          hora: '10:30',
          enviado: false
        }
      ]
    },
    {
      id: 2,
      nombre: 'Sigma Boy',
      avatar: 'S',
      imagen: 'https://i.pinimg.com/236x/96/53/f7/9653f749c03eb888c0dde626b925437b.jpg',
      mensajes: [
        {
          texto: 'Thomi tiene sida',
          hora: '11:20',
          enviado: false
        }
      ]
    },
    {
      id: 3,
      nombre: 'Sofía',
      avatar: 'S',
      imagen: '',
      mensajes: [
        {
          texto: '¡Gracias!',
          hora: '12:15',
          enviado: false
        }
      ]
    }
  ];

  contactos = [
    {
      nombre: 'Camila',
      avatar: 'C',
      imagen: ''
    },
    {
      nombre: 'Messi',
      avatar: 'M',
      imagen: 'https://i.pinimg.com/736x/a9/e8/d0/a9e8d037b5d81118c5486d1a46176254.jpg'
    }
  ];

  /**
   * Inicializa los servicios de autenticación y navegación.
   */
  constructor(
      private auth: AuthService,
      private router: Router
  ) {}

  /**
   * Obtiene el nombre del usuario autenticado.
   */
  ngOnInit(): void {
    this.miUsuario = this.auth.obtenerUsuario() || 'Usuario';
  }

  /**
   * Retorna el chat seleccionado actualmente.
   */
  get chatActual(): Chat | undefined {
    return this.chats.find(
        chat => chat.id === this.chatSeleccionado
    );
  }

  /**
   * Filtra las conversaciones según la búsqueda.
   */
  get chatsFiltrados(): Chat[] {
    return this.chats.filter(chat =>
        chat.nombre.toLowerCase().includes(
            this.busquedaChat.toLowerCase()
        )
    );
  }

  /**
   * Obtiene el último mensaje de una conversación.
   * @param chat Conversación que se desea consultar.
   */
  ultimoMensaje(chat: Chat): string {
    if (chat.mensajes.length === 0) {
      return 'Sin mensajes';
    }

    return chat.mensajes[chat.mensajes.length - 1].texto;
  }

  /**
   * Abre una conversación seleccionada.
   * @param id Identificador del chat.
   */
  abrirChat(id: number): void {
    this.chatSeleccionado = id;
    this.mensajeNuevo = '';
  }

  /**
   * Envía un mensaje al chat seleccionado.
   */
  enviarMensaje(): void {
    const texto = this.mensajeNuevo.trim();
    const chat = this.chatActual;

    if (texto === '' || !chat) {
      return;
    }

    const hora = new Date().toLocaleTimeString('es-CO', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });

    chat.mensajes.push({
      texto: texto,
      hora: hora,
      enviado: true
    });

    this.mensajeNuevo = '';
  }

  /**
   * Cambia la sección visible del panel.
   * @param nuevaSeccion Sección que se desea mostrar.
   */
  irASeccion(
      nuevaSeccion: 'chats' | 'ia' | 'contactos'
  ): void {
    this.seccion = nuevaSeccion;
  }

  /**
   * Cierra la sesión y redirige al inicio de sesión.
   */
  salir(): void {
    this.auth.cerrarSesion();
    this.router.navigate(['/login']);
  }
}
