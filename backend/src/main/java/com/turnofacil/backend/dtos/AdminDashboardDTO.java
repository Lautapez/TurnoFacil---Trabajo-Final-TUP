package com.turnofacil.backend.dtos;

import java.util.Map;

public record AdminDashboardDTO(
        long totalUsuarios,
        long clientes,
        long profesionales,
        long administradores,
        long profesionalesActivos,
        long turnosHoy,
        Map<String, Long> turnosHoyPorEstado,
        Map<String, Long> turnosPorEstado
) { }
