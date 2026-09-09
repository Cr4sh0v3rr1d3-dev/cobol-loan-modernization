package com.portfolio.prestamos.repository;

import com.portfolio.prestamos.entity.TasaBancaria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TasaBancariaRepositoryTest {

    @Autowired
    private TasaBancariaRepository tasaBancariaRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void resuelveLaTasaMasRecienteCuandoHayMasDeUnSnapshotParaElMismoBancoYProducto() {
        insertarTasa("Banco Galicia", "Eminent", "35.000", "2026-01-01");
        insertarTasa("Banco Galicia", "Eminent", "39.000", "2026-09-02");

        Optional<TasaBancaria> resultado = tasaBancariaRepository.resolverTasaVigente("Banco Galicia", "Eminent");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTna()).isEqualByComparingTo(new BigDecimal("39.000"));
    }

    @Test
    void noEncuentraTasaParaUnaCombinacionInexistenteDeBancoYProducto() {
        insertarTasa("Banco Galicia", "Eminent", "39.000", "2026-09-02");

        Optional<TasaBancaria> resultado = tasaBancariaRepository.resolverTasaVigente("Banco Inexistente", "X");

        assertThat(resultado).isEmpty();
    }

    private void insertarTasa(String banco, String producto, String tna, String fechaVigencia) {
        jdbcTemplate.update(
                "INSERT INTO tasas_bancarias (banco, producto, tna, cft, fecha_vigencia, fuente) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                banco, producto, tna, tna, fechaVigencia, "https://ejemplo.test/tasas");
    }
}
