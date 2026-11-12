package com.turnofacil.backend.dtos;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoDTO(@NotNull(message = "El campo activo es obligatorio") Boolean activo) { }
