package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.TurnoRegistroDTO;
import com.turnofacil.backend.dtos.TurnoReprogramacionDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.services.TurnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Autorización en dos capas: el rol se exige acá con @PreAuthorize y la propiedad del turno
 * (que el turno pertenezca al usuario del token) se valida en TurnoService.
 */
@RestController
@RequestMapping("/api/appointments")
public class TurnoController {

    @Autowired
    private TurnoService turnoService;

    // POST /api/appointments -> Un CLIENTE reserva para sí mismo (clienteId sale del token)
    @PreAuthorize("hasRole('CLIENTE')")
    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(@Valid @RequestBody TurnoRegistroDTO dto,
                                                       @AuthenticationPrincipal Usuario actual) {
        TurnoResponseDTO nuevoTurno = turnoService.crearTurno(actual.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTurno);
    }

    // GET /api/appointments/user/{id} -> Historial del cliente (solo el propio usuario o ADMINISTRADOR)
    @GetMapping("/user/{id}")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosPorUsuario(@PathVariable Long id,
                                                                         @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.listarTurnosPorUsuario(id, actual));
    }

    // GET /api/appointments/professional/{id} -> Dashboard del profesional (HU04)
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMINISTRADOR')")
    @GetMapping("/professional/{id}")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosPorProfesional(@PathVariable Long id,
                                                                             @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.listarTurnosPorProfesional(id, actual));
    }

    // PUT /api/appointments/{id}/cancel -> Cancelación lógica (RN-04)
    @PutMapping("/{id}/cancel")
    public ResponseEntity<TurnoResponseDTO> cancelarTurno(@PathVariable Long id,
                                                          @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.cancelarTurno(id, actual));
    }

    // PUT /api/appointments/{id}/reschedule -> Reprogramación (RN-04)
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<TurnoResponseDTO> reprogramarTurno(@PathVariable Long id,
                                                             @Valid @RequestBody TurnoReprogramacionDTO dto,
                                                             @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.reprogramarTurno(id, dto, actual));
    }

    // PUT /api/appointments/{id}/confirm -> Solo el profesional del turno o ADMINISTRADOR
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMINISTRADOR')")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<TurnoResponseDTO> confirmarTurno(@PathVariable Long id,
                                                           @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.confirmarTurno(id, actual));
    }

    // PUT /api/appointments/{id}/finish -> Solo el profesional del turno o ADMINISTRADOR
    @PreAuthorize("hasAnyRole('PROFESIONAL','ADMINISTRADOR')")
    @PutMapping("/{id}/finish")
    public ResponseEntity<TurnoResponseDTO> finalizarTurno(@PathVariable Long id,
                                                           @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(turnoService.finalizarTurno(id, actual));
    }
}
