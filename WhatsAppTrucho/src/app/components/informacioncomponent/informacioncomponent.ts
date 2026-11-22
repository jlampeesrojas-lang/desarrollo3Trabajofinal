import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {AuthService} from "../../services/authservice";
import {PreguntaFrecuente} from "../../models/preguntafrecuente";

/** Pantalla con la información de la empresa, las preguntas frecuentes y los datos de contacto. */
@Component({
  selector: "app-informacioncomponent",
  standalone: false,
  styleUrl: "./informacioncomponent.css",
  templateUrl: "./informacioncomponent.html",
})
export class Informacioncomponent implements OnInit {
  abierta: number | null = null;

  preguntas: PreguntaFrecuente[] = [
    {
      pregunta: "¿Qué es Sigma Chat?",
      respuesta: "Sigma Chat es una aplicación de mensajería desarrollada en Colombia que permite conversar con tus contactos, " +
          "enviar imágenes, audios, GIFs y archivos, y consultar a un asistente con inteligencia artificial.",
    },
    {
      pregunta: "¿Mis mensajes están protegidos?",
      respuesta: "Sí. Cada chat tiene su propia llave y los mensajes y archivos se guardan cifrados con AES. " +
          "Las contraseñas se almacenan cifradas, por lo que nadie puede verlas.",
    },
    {
      pregunta: "¿Qué pasa si me equivoco varias veces con la contraseña?",
      respuesta: "Después de 5 intentos fallidos la cuenta se bloquea durante 15 minutos para evitar accesos no autorizados. " +
          "Pasado ese tiempo puedes volver a intentarlo.",
    },
    {
      pregunta: "¿Qué requisitos debe cumplir mi contraseña?",
      respuesta: "Debe tener mínimo 12 caracteres, al menos una letra mayúscula, un número y un símbolo.",
    },
    {
      pregunta: "¿Cómo agrego un contacto?",
      respuesta: "En la pantalla de chats pulsa \"+ Añadir contactos\", luego \"Agregar contacto\" y escribe el nombre de usuario " +
          "o el correo de la persona. Puedes ponerle un alias para reconocerla más fácil.",
    },
    {
      pregunta: "¿Qué puedo enviar en un chat?",
      respuesta: "Puedes enviar texto, imágenes, notas de voz, GIFs y archivos. También puedes acompañar un archivo con un texto " +
          "y se mostrará en el mismo mensaje.",
    },
    {
      pregunta: "¿Cómo funciona el chat con la IA?",
      respuesta: "El chat \"IA\" aparece primero en tu lista. Puedes escribirle preguntas o enviarle imágenes, audios y archivos " +
          "de texto, y te responderá en segundos.",
    },
    {
      pregunta: "¿Olvidé mi contraseña, qué hago?",
      respuesta: "En la pantalla de inicio de sesión usa la opción para recuperar la contraseña. Te llegará un correo con los pasos " +
          "para crear una nueva.",
    },
  ];

  constructor(private auth: AuthService, private router: Router) {
  }

  /** Si no hay sesión manda a la página de error 401. */
  ngOnInit() {
    if (!this.auth.estaLogueado()) {
      this.router.navigate(["/error", "401"]);
    }
  }

  /** Vuelve a la pantalla de chats. */
  volver() {
    this.router.navigate(["/principal"]);
  }

  /** Abre la respuesta de una pregunta o la cierra si ya estaba abierta. */
  alternar(indice: number) {
    if (this.abierta === indice) {
      this.abierta = null;
    } else {
      this.abierta = indice;
    }
  }
}