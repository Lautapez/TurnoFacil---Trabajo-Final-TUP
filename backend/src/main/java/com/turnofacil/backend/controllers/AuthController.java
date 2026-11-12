package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.LoginDTO;
import com.turnofacil.backend.dtos.UsuarioRegistroDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Los errores los resuelve GlobalExceptionHandler (409 email duplicado, 401 credenciales, 400 validación)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    // Registro público: siempre crea un CLIENTE
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody UsuarioRegistroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuario(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> login(@Valid @RequestBody LoginDTO loginRequest) {
        return ResponseEntity.ok(usuarioService.login(loginRequest.getEmail(), loginRequest.getPassword()));
    }
}
