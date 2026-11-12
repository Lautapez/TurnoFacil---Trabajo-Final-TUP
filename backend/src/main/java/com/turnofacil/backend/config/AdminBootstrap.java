package com.turnofacil.backend.config;

import com.turnofacil.backend.enums.Rol;
import com.turnofacil.backend.models.Usuario;
import com.turnofacil.backend.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMINISTRADOR a partir de las variables de entorno ADMIN_EMAIL y ADMIN_PASSWORD.
 * El registro publico solo crea CLIENTE; los roles PROFESIONAL y ADMINISTRADOR los asigna un administrador.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (usuarioRepository.existsByEmail(adminEmail)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellido("TurnoFácil");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMINISTRADOR);
        usuarioRepository.save(admin);
    }
}
