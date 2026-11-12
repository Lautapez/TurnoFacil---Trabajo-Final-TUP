package com.turnofacil.backend.dtos;

import com.turnofacil.backend.models.Profesional;

/**
 * Vista pública de un profesional. Nunca incluye la entidad Usuario (ni hash, ni authorities).
 */
public record ProfesionalResponseDTO(
        Long id,
        Long usuarioId,
        String nombre,
        String apellido,
        Long especialidadId,
        String especialidad,
        String telefono,
        Boolean activo
) {
    public static ProfesionalResponseDTO from(Profesional p) {
        return new ProfesionalResponseDTO(
                p.getId(),
                p.getUsuario().getId(),
                p.getUsuario().getNombre(),
                p.getUsuario().getApellido(),
                p.getEspecialidad().getId(),
                p.getEspecialidad().getNombre(),
                p.getTelefono(),
                p.getActivo()
        );
    }
}
