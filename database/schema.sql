CREATE TABLE turnos (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL,
    profesional_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    duracion_minutos INT NOT NULL DEFAULT 30
        CHECK (duracion_minutos > 0),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (
            estado IN (
                'PENDIENTE',
                'CONFIRMADO',
                'CANCELADO',
                'FINALIZADO'
            )
        ),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_turno_cliente
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id),

    CONSTRAINT fk_turno_profesional
        FOREIGN KEY (profesional_id)
        REFERENCES profesionales(id),

    CONSTRAINT no_solapamiento_turnos
        EXCLUDE USING gist (
            profesional_id WITH =,
            tsrange(
                (fecha + hora)::timestamp,
                (
                    fecha + hora +
                    (duracion_minutos * interval '1 minute')
                )::timestamp,
                '[)'
            ) WITH &&
        )
        WHERE (estado != 'CANCELADO')
);
