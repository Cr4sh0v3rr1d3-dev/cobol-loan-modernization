package com.portfolio.prestamos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record PrestamoSimulacionResponse(
        @Schema(description = "Cuota fija mensual calculada por el motor COBOL (sistema francés). Igual para "
                + "todas las cuotas del préstamo.", example = "11335.59")
        BigDecimal cuotaMensual,

        @Schema(description = "Suma de todas las cuotas del préstamo (cuotaMensual × plazoMeses).",
                example = "136027.08")
        BigDecimal totalAPagar,

        @Schema(description = "Total a pagar menos el capital solicitado. Calculado por resta directa, no como "
                + "la suma de los intereses por cuota de tablaAmortizacion — esos dos valores pueden diferir en "
                + "centavos por redondeo independiente en cada cuota (comportamiento real de aritmética fija, "
                + "no un error).", example = "36027.08")
        BigDecimal totalIntereses,

        @Schema(description = "Tasa nominal anual (%) resuelta desde tasas_bancarias para este banco+producto — "
                + "nunca la ingresa el cliente, siempre viene de la tabla.", example = "61.000")
        BigDecimal tasaAnualAplicada,

        @Schema(description = "Nombre del banco tal como está en tasas_bancarias (puede diferir en capitalización "
                + "de lo enviado en el request si la fila tiene otra grafía).", example = "Banco Macro")
        String banco,

        @Schema(description = "Nombre del producto tal como está en tasas_bancarias.", example = "Plan Sueldo")
        String producto,

        @Schema(description = "Desglose interés/amortización de cada cuota del préstamo, calculado por el motor "
                + "COBOL cuota a cuota sobre el saldo pendiente decreciente. Tamaño igual a plazoMeses.")
        List<CuotaAmortizacion> tablaAmortizacion
) {
}
