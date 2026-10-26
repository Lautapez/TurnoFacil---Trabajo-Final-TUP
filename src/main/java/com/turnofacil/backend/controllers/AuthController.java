package com.turnofacil.backend.controllers;

import com.turnofacil.backend.dtos.UsuarioRegistroDTO;
import com.turnofacil.backend.dtos.UsuarioResponseDTO;
import com.turnofacil.backend.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    // POST /api/auth/register -> Registro seguro con DTO
    @PostMapping("/register")
    public ResponseEntity<?> registrar(@Valid @RequestBody UsuarioRegistroDTO dto) {
        try {
            UsuarioResponseDTO nuevoUsuario = usuarioService.registrarUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // POST /api/auth/login -> Autenticación segura
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UsuarioRegistroDTO loginRequest) {
        try {
            UsuarioResponseDTO usuario = usuarioService.login(loginRequest.getEmail(), loginRequest.getPassword());
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}