package com.portfolio.prestamos;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
        title = "Simulador de préstamos",
        description = "Simulación de préstamos personales por sistema francés. El cálculo real vive en un motor "
                + "COBOL; esta API resuelve la tasa vigente y orquesta la invocación.",
        version = "0.1.0"))
@SpringBootApplication
public class PrestamosSimuladorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrestamosSimuladorApiApplication.class, args);
    }
}
