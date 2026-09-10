package com.portfolio.prestamos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CuotaCobolDetalle(
        @JsonProperty("CUOTA-NUMERO") int numero,
        @JsonProperty("CUOTA-INTERES") BigDecimal interes,
        @JsonProperty("CUOTA-AMORTIZACION") BigDecimal amortizacion
) {
}
