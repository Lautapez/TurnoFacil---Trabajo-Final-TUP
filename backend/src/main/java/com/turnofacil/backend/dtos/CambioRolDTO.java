package com.turnofacil.backend.dtos;

import com.turnofacil.backend.enums.Rol;
import jakarta.validation.constraints.NotNull;

public record CambioRolDTO(@NotNull(message = "El rol es obligatorio") Rol rol) { }
