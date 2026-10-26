package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.HorarioRegistroDTO;
import com.turnofacil.backend.dtos.ProfesionalRegistroDTO;
import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.services.ProfesionalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professionals")
public class ProfesionalController {

    @Autowired
    private ProfesionalService profesionalService;

    // GET /api/professionals -> Listado general
    @GetMapping
    public ResponseEntity<List<Profesional>> listarProfesionales(
            @RequestParam(required = false) Long especialidadId) {
        List<Profesional> profesionales;
        if (especialidadId != null) {
            profesionales = profesionalService.listarPorEspecialidad(especialidadId);
        } else {
            profesionales = profesionalService.listarProfesionalesActivos();
        }
        return ResponseEntity.ok(profesionales);
    }

    // POST /api/professionals -> Alta de profesional usando DTO
    @PostMapping
    public ResponseEntity<?> crearProfesional(@Valid @RequestBody ProfesionalRegistroDTO dto) {
        try {
            Profesional nuevoProfesional = profesionalService.guardarProfesionalDTO(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProfesional);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // GET /api/professionals/{id}/availability -> Consulta de horarios
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<Horario>> obtenerHorarios(@PathVariable Long id) {
        List<Horario> horarios = profesionalService.obtenerHorariosPorProfesional(id);
        return ResponseEntity.ok(horarios);
    }

    // POST /api/professionals/availability -> Configura franja horaria usando DTO
    @PostMapping("/availability")
    public ResponseEntity<?> guardarHorario(@Valid @RequestBody HorarioRegistroDTO dto) {
        try {
            Horario nuevoHorario = profesionalService.guardarHorarioDTO(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoHorario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}