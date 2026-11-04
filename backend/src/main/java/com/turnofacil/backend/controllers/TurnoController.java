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

    // POST /api/appointments -> Solicita y bloquea un nuevo turno de forma segura (RN-01 a RN-03)
    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(@Valid @RequestBody TurnoRegistroDTO dto) {
        TurnoResponseDTO nuevoTurno = turnoService.crearTurno(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTurno);
    }

    // GET /api/appointments/user/{id} -> Obtiene el historial de turnos de un cliente en DTO
    @GetMapping("/user/{id}")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosPorUsuario(@PathVariable Long id) {
        List<TurnoResponseDTO> turnos = turnoService.listarTurnosPorUsuario(id);
        return ResponseEntity.ok(turnos);
    }

    // GET /api/appointments/professional/{id} -> Endpoint requerido para el Dashboard del Profesional (HU04)
    @GetMapping("/professional/{id}")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosPorProfesional(@PathVariable Long id) {
        List<TurnoResponseDTO> turnos = turnoService.listarTurnosPorProfesional(id);
        return ResponseEntity.ok(turnos);
    }

    // PUT /api/appointments/{id}/cancel -> Realiza la cancelación lógica utilizando DTO
    @PutMapping("/{id}/cancel")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(@PathVariable Long id) {
        TurnoResponseDTO turnoCancelado = turnoService.cancelarTurno(id);
        return ResponseEntity.ok(turnoCancelado);
    }

    // PUT /api/appointments/{id}/confirm -> Confirma un turno pendiente
    @PutMapping("/{id}/confirm")
    public ResponseEntity<TurnoResponseDTO> confirmarTurno(@PathVariable Long id) {
        TurnoResponseDTO turnoConfirmado = turnoService.confirmarTurno(id);
        return ResponseEntity.ok(turnoConfirmado);
    }

    // PUT /api/appointments/{id}/finish -> Finaliza un turno activo o confirmado
    @PutMapping("/{id}/finish")
    public ResponseEntity<TurnoResponseDTO> finalizarTurno(@PathVariable Long id) {
        TurnoResponseDTO turnoFinalizado = turnoService.finalizarTurno(id);
        return ResponseEntity.ok(turnoFinalizado);
    }
}