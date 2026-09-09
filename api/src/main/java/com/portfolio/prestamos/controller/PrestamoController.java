package com.portfolio.prestamos.controller;

import com.portfolio.prestamos.dto.PrestamoSimulacionRequest;
import com.portfolio.prestamos.dto.PrestamoSimulacionResponse;
import com.portfolio.prestamos.service.PrestamoSimulacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prestamos")
public class PrestamoController {

    private final PrestamoSimulacionService prestamoSimulacionService;

    public PrestamoController(PrestamoSimulacionService prestamoSimulacionService) {
        this.prestamoSimulacionService = prestamoSimulacionService;
    }

    @PostMapping("/simular")
    public ResponseEntity<PrestamoSimulacionResponse> simular(@Valid @RequestBody PrestamoSimulacionRequest request) {
        return ResponseEntity.ok(prestamoSimulacionService.simularPrestamo(request));
    }
}
