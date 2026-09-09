package com.portfolio.prestamos;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta la imagen real de docker/Dockerfile (COBOL compilado desde fuente +
 * Spring Boot) contra un Postgres real y pega un request HTTP real de punta a
 * punta. Este es el criterio de "listo" del proyecto (ver CLAUDE.md sección 5),
 * no los unit tests sueltos de las otras clases de test.
 */
@Testcontainers
class PrestamoSimulacionEndToEndTest {

    private static final Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("cobol_loans")
            .withUsername("prestamos_test")
            .withPassword("prestamos_test");

    private static final Path REPO_ROOT = Path.of("..").toAbsolutePath().normalize();

    // Contexto acotado a lo que el Dockerfile realmente COPY-ea (cobol/, db/,
    // api/) en vez de toda la raíz del repo — incluir .git/.claude ahí colgó
    // el tar recursivo de Testcontainers sin avanzar.
    static ImageFromDockerfile appImage = new ImageFromDockerfile("prestamos-simulador-e2e-test", false)
            .withFileFromPath("cobol", REPO_ROOT.resolve("cobol"))
            .withFileFromPath("db", REPO_ROOT.resolve("db"))
            .withFileFromPath("api", REPO_ROOT.resolve("api"))
            .withFileFromPath("Dockerfile", REPO_ROOT.resolve("docker/Dockerfile"));

    @Container
    static GenericContainer<?> api = new GenericContainer<>(appImage)
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/cobol_loans")
            .withEnv("SPRING_DATASOURCE_USERNAME", "prestamos_test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "prestamos_test")
            .waitingFor(Wait.forLogMessage(".*Started PrestamosSimuladorApiApplication.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(3))
            .withLogConsumer((OutputFrame frame) -> System.out.print(frame.getUtf8String()))
            .dependsOn(postgres);

    @Test
    void simulaPrestamoContraElStackCompletoRealCobolMasPostgres() {
        var client = RestClient.create("http://" + api.getHost() + ":" + api.getMappedPort(8080));

        var response = client.post()
                .uri("/api/v1/prestamos/simular")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"banco":"Banco Macro","producto":"Plan Sueldo","montoSolicitado":100000.00,"plazoMeses":12}
                        """)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"cuotaMensual\":11335.59");
        assertThat(response.getBody()).contains("\"tasaAnualAplicada\":61.000");
    }

    @Test
    void bancoOProductoInexistenteDevuelveNotFoundExplicito() {
        var client = RestClient.create("http://" + api.getHost() + ":" + api.getMappedPort(8080));

        var response = client.post()
                .uri("/api/v1/prestamos/simular")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"banco":"Banco Inventado","producto":"NoExiste","montoSolicitado":100000.00,"plazoMeses":12}
                        """)
                .retrieve()
                .onStatus(status -> true, (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
