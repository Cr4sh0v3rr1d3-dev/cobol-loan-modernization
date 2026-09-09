package com.portfolio.prestamos.exception;

public class SimulacionCobolException extends RuntimeException {

    public SimulacionCobolException(String mensaje) {
        super(mensaje);
    }

    public SimulacionCobolException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
