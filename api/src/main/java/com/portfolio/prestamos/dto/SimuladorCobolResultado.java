package com.portfolio.prestamos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SimuladorCobolResultado(
        @JsonProperty("RES-CUOTA-MENSUAL") BigDecimal cuotaMensual,
        @JsonProperty("RES-TOTAL-A-PAGAR") BigDecimal totalAPagar,
        @JsonProperty("RES-TOTAL-INTERESES") BigDecimal totalIntereses,
        @JsonProperty("RES-COD-ERROR") int codigoError,
        @JsonProperty("RES-MENSAJE-ERROR") String mensajeError,
        List<CuotaCobolDetalle> tablaAmortizacion
) {
    public SimuladorCobolResultado {
        if (tablaAmortizacion == null) {
            tablaAmortizacion = List.of();
        }
    }

    public boolean tieneError() {
        return codigoError != 0;
    }

    public String mensajeErrorLimpio() {
        return mensajeError == null ? "" : mensajeError.trim();
    }

    public SimuladorCobolResultado conTabla(List<CuotaCobolDetalle> tabla) {
        return new SimuladorCobolResultado(cuotaMensual, totalAPagar, totalIntereses, codigoError, mensajeError, tabla);
    }
}
