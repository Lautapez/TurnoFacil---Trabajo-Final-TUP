package com.turnofacil.backend.dtos;

import com.turnofacil.backend.models.Especialidad;

public record EspecialidadResponseDTO(Long id, String nombre) {
    public static EspecialidadResponseDTO from(Especialidad e) {
        return new EspecialidadResponseDTO(e.getId(), e.getNombre());
    }
}
