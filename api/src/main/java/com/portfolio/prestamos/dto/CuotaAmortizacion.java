package com.portfolio.prestamos.dto;

import java.math.BigDecimal;

public record CuotaAmortizacion(int numero, BigDecimal interes, BigDecimal amortizacion) {
}
