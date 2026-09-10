package com.portfolio.prestamos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Cuerpo de error real armado por GlobalExceptionHandler para los codigos 400, 404 y 503")
public record ErrorResponse(
        @Schema(description = "Momento en que se generó el error") Instant timestamp,
        @Schema(description = "Mensaje de negocio legible — nunca un stack trace ni un detalle interno") String error
) {
}
