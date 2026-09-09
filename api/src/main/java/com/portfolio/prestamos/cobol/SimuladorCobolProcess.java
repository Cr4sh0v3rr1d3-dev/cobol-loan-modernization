package com.portfolio.prestamos.cobol;

import com.portfolio.prestamos.dto.SimuladorCobolResultado;
import com.portfolio.prestamos.exception.SimulacionCobolException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class SimuladorCobolProcess {

    private static final long TIMEOUT_SEGUNDOS = 10;

    private final String executablePath;
    private final ObjectMapper objectMapper;

    public SimuladorCobolProcess(
            @Value("${simulador.cobol.executable-path:/app/bin/simloan}") String executablePath,
            ObjectMapper objectMapper) {
        this.executablePath = executablePath;
        this.objectMapper = objectMapper;
    }

    public SimuladorCobolResultado ejecutar(BigDecimal montoSolicitado, int plazoMeses, BigDecimal tasaAnual) {
        String lineaEntrada = "%s,%d,%s".formatted(
                montoSolicitado.setScale(2, RoundingMode.HALF_UP),
                plazoMeses,
                tasaAnual.setScale(3, RoundingMode.HALF_UP));

        Process proceso = iniciarProceso();
        try {
            escribirEntrada(proceso, lineaEntrada);
            String salida = leerSalida(proceso);
            esperarFinalizacion(proceso);
            return parsearSalida(salida);
        } finally {
            if (proceso.isAlive()) {
                proceso.destroyForcibly();
            }
        }
    }

    private Process iniciarProceso() {
        try {
            return new ProcessBuilder(executablePath).start();
        } catch (IOException e) {
            throw new SimulacionCobolException(
                    "No se pudo iniciar el motor de calculo COBOL en '%s'".formatted(executablePath), e);
        }
    }

    private void escribirEntrada(Process proceso, String lineaEntrada) {
        try (var writer = proceso.getOutputStream()) {
            writer.write((lineaEntrada + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            writer.flush();
        } catch (IOException e) {
            throw new SimulacionCobolException("No se pudo enviar la solicitud al motor de calculo COBOL", e);
        }
    }

    private String leerSalida(Process proceso) {
        try (var reader = new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new SimulacionCobolException("No se pudo leer la respuesta del motor de calculo COBOL", e);
        }
    }

    private void esperarFinalizacion(Process proceso) {
        boolean finalizado;
        try {
            finalizado = proceso.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SimulacionCobolException("La simulacion fue interrumpida", e);
        }
        if (!finalizado) {
            throw new SimulacionCobolException("El motor de calculo COBOL no respondio a tiempo");
        }
    }

    private SimuladorCobolResultado parsearSalida(String salida) {
        if (salida == null || salida.isBlank()) {
            throw new SimulacionCobolException("El motor de calculo COBOL no devolvio resultado");
        }
        try {
            return objectMapper.readValue(salida, SimuladorCobolResultado.class);
        } catch (JacksonException e) {
            throw new SimulacionCobolException("La respuesta del motor de calculo COBOL no es JSON valido: " + salida, e);
        }
    }
}
