-- 1. Tabla de Usuarios (Centraliza la autenticación y control de accesos)
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Hash BCrypt
    rol VARCHAR(30) NOT NULL CHECK (rol IN ('CLIENTE', 'PROFESIONAL', 'ADMINISTRADOR')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de Especialidades (Normalización)
CREATE TABLE especialidades (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- 3. Tabla de Profesionales
CREATE TABLE profesionales (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL UNIQUE,
    especialidad_id INT NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_profesional_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_profesional_especialidad FOREIGN KEY (especialidad_id) REFERENCES especialidades(id)
);

-- 4. Tabla de Horarios (Disponibilidad semanal configurada por el profesional)
CREATE TABLE horarios (
    id SERIAL PRIMARY KEY,
    profesional_id INT NOT NULL,
    dia_semana INT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7), -- 1: Lunes, 7: Domingo
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_horario_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales(id) ON DELETE CASCADE
);

-- 5. Tabla de Turnos (Núcleo transaccional con control real de solapamiento parcial por duración)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE turnos (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL, -- Cliente
    profesional_id INT NOT NULL, -- Profesional
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    duracion_minutos INT NOT NULL DEFAULT 30, -- Duración del turno en minutos
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'CANCELADO', 'FINALIZADO')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_turno_cliente FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_turno_profesional FOREIGN KEY (profesional_id) REFERENCES profesionales(id),
    
    -- Restricción de exclusión: Evita solapamientos parciales de turnos activos usando rangos de tiempo (tsrange)
    CONSTRAINT no_solapamiento_turnos EXCLUDE USING gist (
        profesional_id WITH =,
        tsrange(
            (fecha + hora)::timestamp,
            (fecha + hora + (duracion_minutos * interval '1 minute'))::timestamp,
            '[)'
        ) WITH &&
    ) WHERE (estado != 'CANCELADO')
);
