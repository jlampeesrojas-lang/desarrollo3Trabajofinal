-- =====================================================================
-- Esquema de chats, mensajes y multimedia (Back 2)
-- MySQL 8 / InnoDB / utf8mb4. Depende de la tabla users (schema.sql de Back 1).
-- Equivale a las entidades JPA: Contact, Chat, ChatParticipant, Message, Media.
-- Hibernate (ddl-auto=update) crea las mismas tablas; este script sirve para
-- crearlas a mano o en otro servidor.
-- El texto de los mensajes se guarda cifrado con AESUtil y la llave de cada chat.
-- =====================================================================

-- Lista de contactos de cada usuario
CREATE TABLE IF NOT EXISTS contacts (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  owner_id         BIGINT      NOT NULL,
  contact_user_id  BIGINT      NOT NULL,
  alias            VARCHAR(50) NULL,
  created_at       DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_contacts_owner_contact (owner_id, contact_user_id),
  CONSTRAINT fk_contacts_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_contacts_user  FOREIGN KEY (contact_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Chats: código único y llave AES propia (guardada cifrada)
CREATE TABLE IF NOT EXISTS chats (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  code             VARCHAR(12)  NOT NULL,          -- ej. CH-7KQ2MZ
  key_enc          VARCHAR(120) NOT NULL,          -- llave del chat cifrada con AESUtil
  created_at       DATETIME(6)  NOT NULL,
  last_message_at  DATETIME(6)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_chats_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Participantes de cada chat y hasta cuándo leyeron
CREATE TABLE IF NOT EXISTS chat_participants (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  chat_id       BIGINT      NOT NULL,
  user_id       BIGINT      NOT NULL,
  joined_at     DATETIME(6) NOT NULL,
  last_read_at  DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_chat_participants (chat_id, user_id),
  CONSTRAINT fk_cp_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
  CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Imágenes y audios
CREATE TABLE IF NOT EXISTS media (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  chat_id      BIGINT      NOT NULL,
  uploader_id  BIGINT      NOT NULL,
  mime_type    VARCHAR(50) NOT NULL,
  size_bytes   BIGINT      NOT NULL,
  data_enc     LONGBLOB    NOT NULL,
  created_at   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_media_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
  CONSTRAINT fk_media_user FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Mensajes: el texto nunca se guarda en claro
CREATE TABLE IF NOT EXISTS messages (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  chat_id      BIGINT      NOT NULL,
  sender_id    BIGINT      NULL,                   -- NULL = mensaje de la IA
  type         VARCHAR(10) NOT NULL,               -- TEXTO, IMAGEN, AUDIO, IA
  content_enc  TEXT        NOT NULL,               -- texto cifrado con AESUtil
  media_id     BIGINT      NULL,
  created_at   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_messages_chat (chat_id, id),
  CONSTRAINT fk_messages_chat   FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT fk_messages_media  FOREIGN KEY (media_id) REFERENCES media (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;