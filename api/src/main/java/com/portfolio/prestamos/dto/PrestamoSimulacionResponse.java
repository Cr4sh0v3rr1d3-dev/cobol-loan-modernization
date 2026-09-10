package com.portfolio.prestamos.dto;

import java.math.BigDecimal;
import java.util.List;

public record PrestamoSimulacionResponse(
        BigDecimal cuotaMensual,
        BigDecimal totalAPagar,
        BigDecimal totalIntereses,
        BigDecimal tasaAnualAplicada,
        String banco,
        String producto,
        List<CuotaAmortizacion> tablaAmortizacion
) {
}
