package com.turnofacil.backend.dtos;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoReprogramacionDTO(
        @NotNull(message = "La fecha es obligatoria")
        @FutureOrPresent(message = "La fecha del turno no puede ser en el pasado")
        LocalDate fecha,

        @NotNull(message = "La hora es obligatoria")
        LocalTime hora,

        // Opcional: si no se envía se conserva la duración original del turno
        @Min(value = 1, message = "La duración en minutos debe ser mayor a cero")
        Integer duracionMinutos
) { }
