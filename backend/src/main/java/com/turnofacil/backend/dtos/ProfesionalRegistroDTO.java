package com.turnofacil.backend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class ProfesionalRegistroDTO {

    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El ID de especialidad es obligatorio")
    private Long especialidadId;

    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    // Getters y Setters
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getEspecialidadId() { return especialidadId; }
    public void setEspecialidadId(Long especialidadId) { this.especialidadId = especialidadId; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}