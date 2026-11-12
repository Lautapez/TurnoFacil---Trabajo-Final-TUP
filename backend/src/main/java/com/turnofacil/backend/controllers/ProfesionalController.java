package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.HorarioRegistroDTO;
import com.turnofacil.backend.dtos.HorarioResponseDTO;
import com.turnofacil.backend.dtos.ProfesionalRegistroDTO;
import com.turnofacil.backend.dtos.ProfesionalResponseDTO;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.services.ProfesionalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Los errores los resuelve GlobalExceptionHandler
@RestController
@RequestMapping("/api/professionals")
public class ProfesionalController {

    @Autowired
    private ProfesionalService profesionalService;

    // GET /api/professionals -> Listado de profesionales activos (cualquier usuario autenticado)
    @GetMapping
    public ResponseEntity<List<ProfesionalResponseDTO>> listarProfesionales(
            @RequestParam(required = false) Long especialidadId) {
        List<ProfesionalResponseDTO> profesionales = especialidadId != null
                ? profesionalService.listarPorEspecialidad(especialidadId)
                : profesionalService.listarProfesionalesActivos();
        return ResponseEntity.ok(profesionales);
    }

    // GET /api/professionals/me -> Perfil profesional del usuario autenticado
    @PreAuthorize("hasRole('PROFESIONAL')")
    @GetMapping("/me")
    public ResponseEntity<ProfesionalResponseDTO> miPerfil(@AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(profesionalService.obtenerPerfilDe(actual));
    }

    // POST /api/professionals -> Alta del perfil profesional (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<ProfesionalResponseDTO> crearProfesional(@Valid @RequestBody ProfesionalRegistroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesionalService.guardarProfesionalDTO(dto));
    }

    // PUT /api/professionals/{id}/deactivate -> Baja lógica (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> darDeBajaProfesional(@PathVariable Long id) {
        profesionalService.darDeBajaProfesional(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/professionals/{id}/activate -> Reactivación (solo ADMINISTRADOR)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> reactivarProfesional(@PathVariable Long id) {
        profesionalService.reactivarProfesional(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/professionals/{id}/availability -> Consulta de horarios (cualquier usuario autenticado)
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<HorarioResponseDTO>> obtenerHorarios(@PathVariable Long id) {
        return ResponseEntity.ok(profesionalService.obtenerHorariosPorProfesional(id));
    }

    // POST /api/professionals/availability -> Carga una franja en la agenda del profesional autenticado
    @PreAuthorize("hasRole('PROFESIONAL')")
    @PostMapping("/availability")
    public ResponseEntity<HorarioResponseDTO> guardarHorario(@Valid @RequestBody HorarioRegistroDTO dto,
                                                             @AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesionalService.guardarHorarioDTO(dto, actual));
    }
}
