-- =====================================================================
-- Esquema de seguridad, usuarios y auditoria (Back 1)
-- MySQL 8 / InnoDB / utf8mb4
-- Equivale a las entidades JPA: User, EmailToken, RefreshToken, AuditLog.
-- Hibernate (spring.jpa.hibernate.ddl-auto=update) crea las mismas tablas
-- automaticamente; este script sirve para crearlas a mano o en otro servidor.
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
  id                      BIGINT       NOT NULL AUTO_INCREMENT,
  username                VARCHAR(50)  NOT NULL,
  email                   VARCHAR(150) NOT NULL,
  password_hash           VARCHAR(100) NOT NULL,            -- hash BCrypt, nunca texto plano
  role                    VARCHAR(20)  NOT NULL,            -- 'USER' o 'ADMIN'
  email_verified          BIT(1)       NOT NULL DEFAULT 0,
  account_non_expired     BIT(1)       NOT NULL DEFAULT 1,
  account_non_locked      BIT(1)       NOT NULL DEFAULT 1,
  credentials_non_expired BIT(1)       NOT NULL DEFAULT 1,
  enabled                 BIT(1)       NOT NULL DEFAULT 1,
  created_at              DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tokens de un solo uso enviados por correo (se guarda solo el hash SHA-256)
CREATE TABLE IF NOT EXISTS email_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  token_hash  VARCHAR(64) NOT NULL,
  type        VARCHAR(30) NOT NULL,                         -- 'CONFIRM_EMAIL' o 'RESET_PASSWORD'
  expires_at  DATETIME(6) NOT NULL,
  used_at     DATETIME(6) NULL,
  created_at  DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_email_tokens_hash (token_hash),
  KEY idx_email_tokens_user (user_id),
  CONSTRAINT fk_email_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sesiones (refresh tokens, solo hash)
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  token_hash  VARCHAR(64) NOT NULL,
  expires_at  DATETIME(6) NOT NULL,
  revoked     BIT(1)      NOT NULL DEFAULT 0,
  created_at  DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_tokens_hash (token_hash),
  KEY idx_refresh_tokens_user (user_id),
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Auditoria: sin llave foranea a proposito, el historial sobrevive si se borra el usuario
CREATE TABLE IF NOT EXISTS audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NULL,
  action      VARCHAR(40)  NOT NULL,
  ip          VARCHAR(45)  NULL,
  location    VARCHAR(150) NULL,                            -- se llena el 8 de nov
  detail      VARCHAR(500) NULL,                            -- metadatos, nunca contenido de mensajes
  created_at  DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_audit_user (user_id),
  KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
