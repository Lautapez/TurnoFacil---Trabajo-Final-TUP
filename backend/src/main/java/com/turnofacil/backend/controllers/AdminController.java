package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.AdminDashboardDTO;
import com.turnofacil.backend.dtos.CambioEstadoDTO;
import com.turnofacil.backend.dtos.CambioRolDTO;
import com.turnofacil.backend.dtos.ProfesionalResponseDTO;
import com.turnofacil.backend.dtos.TurnoResponseDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.services.AdminService;
import com.turnofacil.backend.services.ProfesionalService;
import com.turnofacil.backend.services.TurnoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Módulo administrativo: todo el controlador exige rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ProfesionalService profesionalService;

    @Autowired
    private TurnoService turnoService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDTO> dashboard() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UsuarioResponseDTO> cambiarRol(@PathVariable Long id,
                                                         @Valid @RequestBody CambioRolDTO dto,
                                                         @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(adminService.cambiarRol(id, dto.rol(), actual));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UsuarioResponseDTO> cambiarEstado(@PathVariable Long id,
                                                            @Valid @RequestBody CambioEstadoDTO dto,
                                                            @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(adminService.cambiarEstado(id, dto.activo(), actual));
    }

    // Todos los profesionales, activos e inactivos
    @GetMapping("/professionals")
    public ResponseEntity<List<ProfesionalResponseDTO>> listarProfesionales() {
        return ResponseEntity.ok(profesionalService.listarTodos());
    }

    // Todos los turnos, opcionalmente de una fecha (?fecha=yyyy-MM-dd)
    @GetMapping("/appointments")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(turnoService.listarTodos(fecha));
    }
}
