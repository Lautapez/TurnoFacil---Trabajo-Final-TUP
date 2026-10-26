package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.services.TurnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class TurnoController {

    @Autowired
    private TurnoService turnoService;

    // POST /api/appointments -> Solicita y bloquea un nuevo turno de forma segura
    @PostMapping
    public ResponseEntity<?> crearTurno(@Valid @RequestBody TurnoRegistroDTO dto) {
        try {
            TurnoResponseDTO nuevoTurno = turnoService.crearTurno(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTurno);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // GET /api/appointments/user/{id} -> Obtiene el historial de turnos de un cliente en DTO
    @GetMapping("/user/{id}")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosPorUsuario(@PathVariable Long id) {
        List<TurnoResponseDTO> turnos = turnoService.listarTurnosPorUsuario(id);
        return ResponseEntity.ok(turnos);
    }

    // PUT /api/appointments/{id}/cancel -> Realiza la cancelación lógica utilizando DTO
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelarTurno(@PathVariable Long id) {
        try {
            TurnoResponseDTO turnoCancelado = turnoService.cancelarTurno(id);
            return ResponseEntity.ok(turnoCancelado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}