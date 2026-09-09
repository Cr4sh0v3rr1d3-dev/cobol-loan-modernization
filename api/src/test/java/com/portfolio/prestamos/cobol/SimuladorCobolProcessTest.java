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
    void lanzaSimulacionCobolExceptionCuandoElExecutableNoExiste(@TempDir Path tempDir) {
        Path inexistente = tempDir.resolve("no-existe-simloan");
        SimuladorCobolProcess proceso = new SimuladorCobolProcess(inexistente.toString(), objectMapper);

        assertThatThrownBy(() -> proceso.ejecutar(new BigDecimal("1000.00"), 12, new BigDecimal("39.000")))
                .isInstanceOf(SimulacionCobolException.class);
    }

    private Path crearScriptMock(Path dir, String jsonSalida) throws IOException {
        if (esWindows()) {
            Path script = dir.resolve("mock-simloan.cmd");
            Files.writeString(script, "@echo off\r\nset /p linea=\r\necho " + jsonSalida + "\r\n");
            return script;
        }
        Path script = dir.resolve("mock-simloan.sh");
        Files.writeString(script, "#!/bin/sh\nread linea\necho '" + jsonSalida + "'\n");
        if (!script.toFile().setExecutable(true)) {
            throw new IOException("No se pudo marcar el script mock como ejecutable: " + script);
        }
        return script;
    }

    private boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
