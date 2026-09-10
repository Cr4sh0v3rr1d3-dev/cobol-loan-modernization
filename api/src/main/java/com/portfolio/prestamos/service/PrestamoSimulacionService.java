package com.portfolio.prestamos.service;

import com.portfolio.prestamos.cobol.SimuladorCobolProcess;
import com.portfolio.prestamos.dto.CuotaAmortizacion;
import com.portfolio.prestamos.dto.PrestamoSimulacionRequest;
import com.portfolio.prestamos.dto.PrestamoSimulacionResponse;
import com.portfolio.prestamos.dto.SimuladorCobolResultado;
import com.portfolio.prestamos.entity.TasaBancaria;
import com.portfolio.prestamos.exception.NegocioException;
import com.portfolio.prestamos.repository.TasaBancariaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrestamoSimulacionService {

    private final TasaBancariaRepository tasaBancariaRepository;
    private final SimuladorCobolProcess simuladorCobolProcess;

    public PrestamoSimulacionService(
            TasaBancariaRepository tasaBancariaRepository,
            SimuladorCobolProcess simuladorCobolProcess) {
        this.tasaBancariaRepository = tasaBancariaRepository;
        this.simuladorCobolProcess = simuladorCobolProcess;
    }

    public PrestamoSimulacionResponse simularPrestamo(PrestamoSimulacionRequest request) {
        TasaBancaria tasaVigente = resolverTasa(request.banco(), request.producto());

        SimuladorCobolResultado resultado = simuladorCobolProcess.ejecutar(
                request.montoSolicitado(), request.plazoMeses(), tasaVigente.getTna());

        if (resultado.tieneError()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, mensajeDeError(resultado));
        }

        return new PrestamoSimulacionResponse(
                resultado.cuotaMensual(),
                resultado.totalAPagar(),
                resultado.totalIntereses(),
                tasaVigente.getTna(),
                tasaVigente.getBanco(),
                tasaVigente.getProducto(),
                mapearTabla(resultado));
    }

    private List<CuotaAmortizacion> mapearTabla(SimuladorCobolResultado resultado) {
        return resultado.tablaAmortizacion().stream()
                .map(cuota -> new CuotaAmortizacion(cuota.numero(), cuota.interes(), cuota.amortizacion()))
                .toList();
    }

    private TasaBancaria resolverTasa(String banco, String producto) {
        return tasaBancariaRepository.resolverTasaVigente(banco, producto)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND,
                        "No existe tasa vigente para banco '%s' y producto '%s'".formatted(banco, producto)));
    }

    private String mensajeDeError(SimuladorCobolResultado resultado) {
        String mensaje = resultado.mensajeErrorLimpio();
        return mensaje.isEmpty()
                ? "Error de calculo en el motor COBOL (codigo %d)".formatted(resultado.codigoError())
                : mensaje;
    }
}
