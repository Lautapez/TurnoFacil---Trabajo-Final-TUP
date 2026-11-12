package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.EspecialidadRequestDTO;
import com.turnofacil.backend.dtos.EspecialidadResponseDTO;
import com.turnofacil.backend.services.EspecialidadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class EspecialidadController {

    @Autowired
    private EspecialidadService especialidadService;

    // GET /api/specialties -> cualquier usuario autenticado
    @GetMapping
    public ResponseEntity<List<EspecialidadResponseDTO>> listar() {
        return ResponseEntity.ok(especialidadService.listar());
    }

    // POST /api/specialties -> solo ADMINISTRADOR
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<EspecialidadResponseDTO> crear(@Valid @RequestBody EspecialidadRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crear(dto));
    }
}
