package com.turnofacil.backend.controllers;

import com.turnofacil.backend.models.Horario;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.services.ProfesionalService;
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

    // GET /api/professionals -> Obtiene el listado general de profesionales activos o filtrados por especialidad
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

    // POST /api/professionals -> Da de alta un nuevo perfil profesional
    @PostMapping
    public ResponseEntity<Profesional> crearProfesional(@RequestBody Profesional profesional) {
        Profesional nuevoProfesional = profesionalService.guardarProfesional(profesional);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoProfesional);
    }

    // GET /api/professionals/{id}/availability -> Consulta las franjas horarias de un profesional
    @GetMapping("/{id}/availability")
    public ResponseEntity<List<Horario>> obtenerHorarios(@PathVariable Long id) {
        List<Horario> horarios = profesionalService.obtenerHorariosPorProfesional(id);
        return ResponseEntity.ok(horarios);
    }

    // POST /api/professionals/availability -> Configura y guarda una nueva franja horaria
    @PostMapping("/availability")
    public ResponseEntity<Horario> guardarHorario(@RequestBody Horario horario) {
        Horario nuevoHorario = profesionalService.guardarHorario(horario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoHorario);
    }
}