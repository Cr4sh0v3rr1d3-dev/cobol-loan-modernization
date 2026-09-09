package com.portfolio.prestamos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SimuladorCobolResultado(
        @JsonProperty("RES-CUOTA-MENSUAL") BigDecimal cuotaMensual,
        @JsonProperty("RES-TOTAL-A-PAGAR") BigDecimal totalAPagar,
        @JsonProperty("RES-TOTAL-INTERESES") BigDecimal totalIntereses,
        @JsonProperty("RES-COD-ERROR") int codigoError,
        @JsonProperty("RES-MENSAJE-ERROR") String mensajeError
) {
    public boolean tieneError() {
        return codigoError != 0;
    }

    public String mensajeErrorLimpio() {
        return mensajeError == null ? "" : mensajeError.trim();
    }
}
