package com.portfolio.prestamos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tasas_bancarias")
public class TasaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String banco;

    @Column(nullable = false)
    private String producto;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal tna;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal cft;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDate fechaVigencia;

    @Column(nullable = false)
    private String fuente;

    protected TasaBancaria() {
    }

    public Long getId() {
        return id;
    }

    public String getBanco() {
        return banco;
    }

    public String getProducto() {
        return producto;
    }

    public BigDecimal getTna() {
        return tna;
    }

    public BigDecimal getCft() {
        return cft;
    }

    public LocalDate getFechaVigencia() {
        return fechaVigencia;
    }

    public String getFuente() {
        return fuente;
    }
}
