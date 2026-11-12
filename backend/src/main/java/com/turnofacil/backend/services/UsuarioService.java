package com.turnofacil.backend.services;

import com.turnofacil.backend.config.JwtUtil;
import com.turnofacil.backend.dtos.UsuarioRegistroDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.exceptions.ConflictoTurnoException;
import com.turnofacil.backend.exceptions.CredencialesInvalidasException;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder; // Bean definido en SecurityConfig

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el email: " + email));
    }

    public UsuarioResponseDTO registrarUsuario(UsuarioRegistroDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictoTurnoException("El email ya se encuentra registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        // El registro público SIEMPRE crea un CLIENTE. PROFESIONAL y ADMINISTRADOR los asigna un administrador.
        usuario.setRol(Rol.CLIENTE);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return mapearAResponseDTO(usuarioGuardado, null);
    }

    public UsuarioResponseDTO login(String email, String rawPassword) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword)
            );
        } catch (AuthenticationException e) {
            throw new CredencialesInvalidasException("Credenciales inválidas.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales inválidas."));

        return mapearAResponseDTO(usuario, token);
    }

    private UsuarioResponseDTO mapearAResponseDTO(Usuario usuario, String token) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getCreatedAt(),
                token
        );
    }
}
