package com.turnofacil.backend.dtos;

import com.turnofacil.backend.enums.EstadoTurno;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TurnoResponseDTO {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long profesionalId;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer duracionMinutos;
    private EstadoTurno estado;
    private LocalDateTime createdAt;
}