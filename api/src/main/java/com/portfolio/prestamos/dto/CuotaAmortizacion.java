package com.portfolio.prestamos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CuotaAmortizacion(
        @Schema(description = "Número de cuota dentro del préstamo, de 1 a plazoMeses.", example = "1")
        int numero,

        @Schema(description = "Porción de esta cuota que corresponde a interés — se calcula sobre el saldo "
                + "pendiente antes de esta cuota, por eso decrece cuota a cuota.", example = "5083.30")
        BigDecimal interes,

        @Schema(description = "Porción de esta cuota que amortiza capital (cuotaMensual - interes de esta "
                + "cuota) — crece cuota a cuota porque cuotaMensual es fija y el interés decrece.",
                example = "6252.29")
        BigDecimal amortizacion
) {
}
