package com.portfolio.prestamos.controller;

import com.portfolio.prestamos.dto.PrestamoSimulacionRequest;
import com.portfolio.prestamos.dto.PrestamoSimulacionResponse;
import com.portfolio.prestamos.exception.NegocioException;
import com.portfolio.prestamos.service.PrestamoSimulacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrestamoController.class)
class PrestamoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PrestamoSimulacionService prestamoSimulacionService;

    @Test
    void devuelve200ConLaSimulacionCuandoLaSolicitudEsValida() throws Exception {
        when(prestamoSimulacionService.simularPrestamo(any())).thenReturn(new PrestamoSimulacionResponse(
                new BigDecimal("10196.72"), new BigDecimal("122360.64"), new BigDecimal("22360.64"),
                new BigDecimal("39.000"), "Banco Galicia", "Eminent", List.of()));

        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Galicia", "Eminent", new BigDecimal("100000.00"), 12);

        mockMvc.perform(post("/api/v1/prestamos/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuotaMensual").value(10196.72))
                .andExpect(jsonPath("$.banco").value("Banco Galicia"));
    }

    @Test
    void devuelve400CuandoElMontoSolicitadoEsInvalido() throws Exception {
        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Galicia", "Eminent", new BigDecimal("0"), 12);

        mockMvc.perform(post("/api/v1/prestamos/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelve404CuandoElServicioReportaQueNoExisteLaTasa() throws Exception {
        when(prestamoSimulacionService.simularPrestamo(any()))
                .thenThrow(new NegocioException(HttpStatus.NOT_FOUND, "No existe tasa vigente"));

        PrestamoSimulacionRequest request = new PrestamoSimulacionRequest(
                "Banco Desconocido", "Producto X", new BigDecimal("1000.00"), 12);

        mockMvc.perform(post("/api/v1/prestamos/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe tasa vigente"));
    }
}
