package com.portfolio.prestamos.controller;

import com.portfolio.prestamos.dto.ErrorResponse;
import com.portfolio.prestamos.dto.PrestamoSimulacionRequest;
import com.portfolio.prestamos.dto.PrestamoSimulacionResponse;
import com.portfolio.prestamos.service.PrestamoSimulacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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

    @Operation(
            summary = "Simula un préstamo personal por sistema francés",
            description = "Resuelve la tasa vigente para el banco/producto solicitado en tasas_bancarias, "
                    + "invoca el motor de cálculo COBOL con esa tasa ya resuelta, y devuelve la cuota mensual, "
                    + "el total a pagar, el total de intereses y la tabla de amortización completa (interés y "
                    + "amortización de cada cuota).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulación calculada correctamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PrestamoSimulacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: falla de validación del request "
                    + "(banco/producto vacíos, monto <= 0, plazo fuera de 1-360) o el motor COBOL rechazó los "
                    + "valores por una regla de negocio propia del cálculo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tasa vigente para el banco y producto "
                    + "solicitados en tasas_bancarias",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "El motor de cálculo COBOL no pudo iniciarse, no "
                    + "respondió a tiempo, o devolvió una salida que no se pudo interpretar",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/simular")
    public ResponseEntity<PrestamoSimulacionResponse> simular(@Valid @RequestBody PrestamoSimulacionRequest request) {
        return ResponseEntity.ok(prestamoSimulacionService.simularPrestamo(request));
    }
}
