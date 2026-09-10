package com.portfolio.prestamos.service;

import com.portfolio.prestamos.cobol.SimuladorCobolProcess;
import com.portfolio.prestamos.dto.CuotaCobolDetalle;
import com.portfolio.prestamos.dto.PrestamoSimulacionRequest;
import com.portfolio.prestamos.dto.PrestamoSimulacionResponse;
import com.portfolio.prestamos.dto.SimuladorCobolResultado;
import com.portfolio.prestamos.entity.TasaBancaria;
import com.portfolio.prestamos.exception.NegocioException;
import com.portfolio.prestamos.repository.TasaBancariaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrestamoSimulacionServiceTest {

    @Mock
    private TasaBancariaRepository tasaBancariaRepository;

    @Mock
    private SimuladorCobolProcess simuladorCobolProcess;

    private PrestamoSimulacionService service;

    private PrestamoSimulacionService construirService() {
        return new PrestamoSimulacionService(tasaBancariaRepository, simuladorCobolProcess);
    }

    @Test
    void simulaUnPrestamoResolviendoLaTasaEInvocandoElMotorCobol() throws Exception {
        service = construirService();
        TasaBancaria tasa = crearTasa("Banco Galicia", "Eminent", new BigDecimal("39.000"));
        when(tasaBancariaRepository.resolverTasaVigente("Banco Galicia", "Eminent"))
                .thenReturn(Optional.of(tasa));
        when(simuladorCobolProcess.ejecutar(eq(new BigDecimal("100000.00")), eq(12), eq(new BigDecimal("39.000"))))
                .thenReturn(new SimuladorCobolResultado(
                        new BigDecimal("10196.72"), new BigDecimal("122360.64"), new BigDecimal("22360.64"), 0, " ",
                        List.of(new CuotaCobolDetalle(1, new BigDecimal("3250.00"), new BigDecimal("6946.72")))));

        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Galicia", "Eminent", new BigDecimal("100000.00"), 12);

        PrestamoSimulacionResponse response = service.simularPrestamo(request);

        assertThat(response.cuotaMensual()).isEqualByComparingTo("10196.72");
        assertThat(response.totalAPagar()).isEqualByComparingTo("122360.64");
        assertThat(response.totalIntereses()).isEqualByComparingTo("22360.64");
        assertThat(response.tasaAnualAplicada()).isEqualByComparingTo("39.000");
        assertThat(response.banco()).isEqualTo("Banco Galicia");
        assertThat(response.producto()).isEqualTo("Eminent");
        assertThat(response.tablaAmortizacion()).hasSize(1);
        assertThat(response.tablaAmortizacion().get(0).numero()).isEqualTo(1);
    }

    @Test
    void lanzaNegocioException404CuandoNoExisteTasaParaBancoYProducto() {
        service = construirService();
        when(tasaBancariaRepository.resolverTasaVigente("Banco Inexistente", "Producto X"))
                .thenReturn(Optional.empty());

        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Inexistente", "Producto X", new BigDecimal("1000.00"), 12);

        assertThatThrownBy(() -> service.simularPrestamo(request))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Banco Inexistente");
    }

    @Test
    void lanzaNegocioException400CuandoElMotorCobolDevuelveCodigoDeError() throws Exception {
        service = construirService();
        TasaBancaria tasa = crearTasa("Banco Macro", "Plan Sueldo", new BigDecimal("61.000"));
        when(tasaBancariaRepository.resolverTasaVigente("Banco Macro", "Plan Sueldo"))
                .thenReturn(Optional.of(tasa));
        when(simuladorCobolProcess.ejecutar(any(), anyInt(), any()))
                .thenReturn(new SimuladorCobolResultado(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 2, "Plazo invalido", List.of()));

        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Macro", "Plan Sueldo", new BigDecimal("1000.00"), 500);

        assertThatThrownBy(() -> service.simularPrestamo(request))
                .isInstanceOf(NegocioException.class)
                .hasMessage("Plazo invalido");
    }

    private TasaBancaria crearTasa(String banco, String producto, BigDecimal tna) throws Exception {
        Constructor<TasaBancaria> constructor = TasaBancaria.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        TasaBancaria tasa = constructor.newInstance();
        setCampo(tasa, "banco", banco);
        setCampo(tasa, "producto", producto);
        setCampo(tasa, "tna", tna);
        setCampo(tasa, "cft", tna);
        setCampo(tasa, "fechaVigencia", LocalDate.of(2026, 9, 2));
        setCampo(tasa, "fuente", "https://ejemplo.test/tasas");
        return tasa;
    }

    private void setCampo(Object objetivo, String nombreCampo, Object valor) throws Exception {
        var campo = TasaBancaria.class.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        campo.set(objetivo, valor);
    }
}
