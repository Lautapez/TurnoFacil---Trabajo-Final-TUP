package com.turnofacil.backend.services;

import com.turnofacil.backend.dtos.UsuarioRegistroDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Registra un nuevo usuario aplicando hash BCrypt y mapeando a DTO de respuesta.
     */
    public UsuarioResponseDTO registrarUsuario(UsuarioRegistroDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya se encuentra registrado.");
        }

        // Mapeamos el DTO a la Entidad JPA
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(dto.getRol() != null ? dto.getRol() : Rol.CLIENTE);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // Retornamos el DTO de respuesta (SIN la contraseña)
        return mapearAResponseDTO(usuarioGuardado);
    }

    /**
     * Autentica las credenciales y retorna un DTO de respuesta seguro.
     */
    public UsuarioResponseDTO login(String email, String rawPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (passwordEncoder.matches(rawPassword, usuario.getPassword())) {
                return mapearAResponseDTO(usuario);
            }
        }

        throw new RuntimeException("Credenciales inválidas.");
    }

    // Método auxiliar para mapear Entidad -> ResponseDTO
    private UsuarioResponseDTO mapearAResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getCreatedAt()
        );
    }
}