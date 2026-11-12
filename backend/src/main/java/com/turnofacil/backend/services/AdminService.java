package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.AdminDashboardDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.enums.EstadoTurno;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.exceptions.RecursoNoEncontradoException;
import com.turnofacil.backend.models.Profesional;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.ProfesionalRepository;
import com.turnofacil.backend.repositories.TurnoRepository;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Módulo administrativo: gestión de usuarios/roles y métricas generales. */
@Service
@Transactional
public class AdminService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::mapear).collect(Collectors.toList());
    }

    /** Asigna un rol a un usuario. Es la única vía para obtener un PROFESIONAL o un ADMINISTRADOR. */
    public UsuarioResponseDTO cambiarRol(Long usuarioId, Rol nuevoRol, Usuario actual) {
        Usuario usuario = obtener(usuarioId);

        if (Objects.equals(usuario.getId(), actual.getId())) {
            throw new ConflictoTurnoException("No podés cambiar tu propio rol.");
        }

        // Un usuario con perfil profesional activo no puede dejar de ser PROFESIONAL sin darlo de baja antes
        if (nuevoRol != Rol.PROFESIONAL) {
            profesionalRepository.findByUsuarioId(usuarioId)
                    .filter(Profesional::getActivo)
                    .ifPresent(p -> {
                        throw new ConflictoTurnoException(
                                "El usuario tiene un perfil profesional activo. Dalo de baja antes de cambiar su rol.");
                    });
        }

        usuario.setRol(nuevoRol);
        return mapear(usuarioRepository.save(usuario));
    }

    /** Activa o desactiva una cuenta. Un usuario inactivo no puede iniciar sesión ni usar su token. */
    public UsuarioResponseDTO cambiarEstado(Long usuarioId, boolean activo, Usuario actual) {
        Usuario usuario = obtener(usuarioId);

        if (Objects.equals(usuario.getId(), actual.getId())) {
            throw new ConflictoTurnoException("No podés modificar el estado de tu propia cuenta.");
        }

        usuario.setActivo(activo);
        return mapear(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO dashboard() {
        LocalDate hoy = LocalDate.now();

        Map<String, Long> hoyPorEstado = new LinkedHashMap<>();
        Map<String, Long> totalPorEstado = new LinkedHashMap<>();
        for (EstadoTurno e : EstadoTurno.values()) {
            hoyPorEstado.put(e.name(), turnoRepository.countByFechaAndEstado(hoy, e));
            totalPorEstado.put(e.name(), turnoRepository.countByEstado(e));
        }

        return new AdminDashboardDTO(
                usuarioRepository.count(),
                usuarioRepository.countByRol(Rol.CLIENTE),
                usuarioRepository.countByRol(Rol.PROFESIONAL),
                usuarioRepository.countByRol(Rol.ADMINISTRADOR),
                profesionalRepository.countByActivoTrue(),
                turnoRepository.countByFecha(hoy),
                hoyPorEstado,
                totalPorEstado
        );
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + id));
    }

    private UsuarioResponseDTO mapear(Usuario u) {
        return new UsuarioResponseDTO(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(),
                u.getRol(), u.getActivo(), u.getCreatedAt(), null);
    }
}
