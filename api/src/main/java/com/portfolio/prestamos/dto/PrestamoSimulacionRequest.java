package com.portfolio.prestamos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PrestamoSimulacionRequest(
        @NotBlank String banco,
        @NotBlank String producto,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoSolicitado,
        @NotNull @Min(1) @Max(360) Integer plazoMeses
) {
}
