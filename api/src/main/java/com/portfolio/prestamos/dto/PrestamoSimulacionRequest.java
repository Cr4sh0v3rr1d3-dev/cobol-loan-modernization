package com.portfolio.prestamos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PrestamoSimulacionRequest(
        @Schema(description = "Nombre del banco tal como figura en tasas_bancarias (ej. 'Banco Macro'). "
                + "Se usa junto con producto para resolver la tasa vigente — no se acepta una tasa manual.",
                example = "Banco Macro")
        @NotBlank String banco,

        @Schema(description = "Nombre del producto de préstamo tal como figura en tasas_bancarias "
                + "(ej. 'Plan Sueldo'). La combinación banco+producto debe existir con una tasa vigente.",
                example = "Plan Sueldo")
        @NotBlank String producto,

        @Schema(description = "Capital solicitado. Entra sin cambios al motor de cálculo COBOL — la validación "
                + "de negocio real (además de este mínimo) la aplica el propio motor.",
                example = "100000.00")
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoSolicitado,

        @Schema(description = "Plazo del préstamo en meses. El rango 1-360 (30 años) es el mismo límite que "
                + "valida SIMLOAN.cbl del lado COBOL — está duplicado a propósito para fallar rápido en la API "
                + "sin gastar un proceso nativo con un valor que el motor va a rechazar igual.",
                example = "12")
        @NotNull @Min(1) @Max(360) Integer plazoMeses
) {
}
