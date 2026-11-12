package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.EspecialidadRequestDTO;
import com.turnofacil.backend.dtos.EspecialidadResponseDTO;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.models.Especialidad;
import com.turnofacil.backend.repositories.EspecialidadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EspecialidadService {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    public List<EspecialidadResponseDTO> listar() {
        return especialidadRepository.findAll().stream()
                .map(EspecialidadResponseDTO::from).collect(Collectors.toList());
    }

    public EspecialidadResponseDTO crear(EspecialidadRequestDTO dto) {
        String nombre = dto.nombre().trim();
        if (especialidadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoTurnoException("Ya existe una especialidad con ese nombre.");
        }
        Especialidad e = new Especialidad();
        e.setNombre(nombre);
        return EspecialidadResponseDTO.from(especialidadRepository.save(e));
    }
}
