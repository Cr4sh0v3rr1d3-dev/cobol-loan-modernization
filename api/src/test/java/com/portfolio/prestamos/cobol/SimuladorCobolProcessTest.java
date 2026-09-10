package com.portfolio.prestamos.cobol;

import com.portfolio.prestamos.dto.SimuladorCobolResultado;
import com.portfolio.prestamos.exception.SimulacionCobolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No hay ejecutable simloan real disponible en este entorno (lo compila la rama
 * de Docker en paralelo). Estos tests generan un script mock descartable que
 * cumple el mismo contrato de stdin/stdout para verificar SimuladorCobolProcess
 * de punta a punta sin depender del binario COBOL real.
 */
class SimuladorCobolProcessTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ejecutaElProcesoYParseaElJsonDeSalida(@TempDir Path tempDir) throws IOException {
        String json = "{\"RES-CUOTA-MENSUAL\":10196.72,\"RES-TOTAL-A-PAGAR\":122360.64,"
                + "\"RES-TOTAL-INTERESES\":22360.64,\"RES-COD-ERROR\":0,\"RES-MENSAJE-ERROR\":\" \"}";
        Path script = crearScriptMock(tempDir, json);
        SimuladorCobolProcess proceso = new SimuladorCobolProcess(script.toString(), objectMapper);

        SimuladorCobolResultado resultado = proceso.ejecutar(
                new BigDecimal("100000.00"), 12, new BigDecimal("39.000"));

        assertThat(resultado.cuotaMensual()).isEqualByComparingTo("10196.72");
        assertThat(resultado.totalAPagar()).isEqualByComparingTo("122360.64");
        assertThat(resultado.totalIntereses()).isEqualByComparingTo("22360.64");
        assertThat(resultado.tieneError()).isFalse();
        assertThat(resultado.mensajeErrorLimpio()).isEmpty();
    }

    @Test
    void mapeaUnCodigoDeErrorDeNegocioDevueltoPorElMotorCobol(@TempDir Path tempDir) throws IOException {
        String json = "{\"RES-CUOTA-MENSUAL\":0,\"RES-TOTAL-A-PAGAR\":0,"
                + "\"RES-TOTAL-INTERESES\":0,\"RES-COD-ERROR\":2,\"RES-MENSAJE-ERROR\":\"Plazo invalido\"}";
        Path script = crearScriptMock(tempDir, json);
        SimuladorCobolProcess proceso = new SimuladorCobolProcess(script.toString(), objectMapper);

        SimuladorCobolResultado resultado = proceso.ejecutar(
                new BigDecimal("100000.00"), 500, new BigDecimal("39.000"));

        assertThat(resultado.tieneError()).isTrue();
        assertThat(resultado.codigoError()).isEqualTo(2);
        assertThat(resultado.mensajeErrorLimpio()).isEqualTo("Plazo invalido");
    }

    @Test
    void parseaLaTablaDeAmortizacionCuandoElMotorDevuelveLineasAdicionales(@TempDir Path tempDir) throws IOException {
        String resumen = "{\"RES-CUOTA-MENSUAL\":10196.72,\"RES-TOTAL-A-PAGAR\":122360.64,"
                + "\"RES-TOTAL-INTERESES\":22360.64,\"RES-COD-ERROR\":0,\"RES-MENSAJE-ERROR\":\" \"}";
        String cuota1 = "{\"CUOTA-NUMERO\":1,\"CUOTA-INTERES\":3250.00,\"CUOTA-AMORTIZACION\":6946.72}";
        String cuota2 = "{\"CUOTA-NUMERO\":2,\"CUOTA-INTERES\":3024.98,\"CUOTA-AMORTIZACION\":7171.74}";
        Path script = crearScriptMock(tempDir, resumen, cuota1, cuota2);
        SimuladorCobolProcess proceso = new SimuladorCobolProcess(script.toString(), objectMapper);

        SimuladorCobolResultado resultado = proceso.ejecutar(
                new BigDecimal("100000.00"), 12, new BigDecimal("39.000"));

        assertThat(resultado.tablaAmortizacion()).hasSize(2);
        assertThat(resultado.tablaAmortizacion().get(0).numero()).isEqualTo(1);
        assertThat(resultado.tablaAmortizacion().get(0).interes()).isEqualByComparingTo("3250.00");
        assertThat(resultado.tablaAmortizacion().get(1).amortizacion()).isEqualByComparingTo("7171.74");
    }

    @Test
    void lanzaSimulacionCobolExceptionCuandoElExecutableNoExiste(@TempDir Path tempDir) {
        Path inexistente = tempDir.resolve("no-existe-simloan");
        SimuladorCobolProcess proceso = new SimuladorCobolProcess(inexistente.toString(), objectMapper);

        assertThatThrownBy(() -> proceso.ejecutar(new BigDecimal("1000.00"), 12, new BigDecimal("39.000")))
                .isInstanceOf(SimulacionCobolException.class);
    }

    private Path crearScriptMock(Path dir, String... lineasSalida) throws IOException {
        if (esWindows()) {
            Path script = dir.resolve("mock-simloan.cmd");
            StringBuilder contenido = new StringBuilder("@echo off\r\nset /p linea=\r\n");
            for (String linea : lineasSalida) {
                contenido.append("echo ").append(linea).append("\r\n");
            }
            Files.writeString(script, contenido.toString());
            return script;
        }
        Path script = dir.resolve("mock-simloan.sh");
        StringBuilder contenido = new StringBuilder("#!/bin/sh\nread linea\n");
        for (String linea : lineasSalida) {
            contenido.append("echo '").append(linea).append("'\n");
        }
        Files.writeString(script, contenido.toString());
        if (!script.toFile().setExecutable(true)) {
            throw new IOException("No se pudo marcar el script mock como ejecutable: " + script);
        }
        return script;
    }

    private boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
