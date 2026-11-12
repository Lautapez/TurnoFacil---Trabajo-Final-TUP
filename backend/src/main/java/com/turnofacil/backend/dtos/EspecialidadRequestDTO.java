package com.turnofacil.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EspecialidadRequestDTO(
        @NotBlank(message = "El nombre de la especialidad es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre
) { }
