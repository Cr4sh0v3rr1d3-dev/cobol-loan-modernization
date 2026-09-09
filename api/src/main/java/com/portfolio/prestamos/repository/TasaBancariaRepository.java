package com.portfolio.prestamos.repository;

import com.portfolio.prestamos.entity.TasaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TasaBancariaRepository extends JpaRepository<TasaBancaria, Long> {

    @Query(value = """
            SELECT * FROM tasas_bancarias
            WHERE banco = :banco AND producto = :producto
            ORDER BY fecha_vigencia DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<TasaBancaria> resolverTasaVigente(@Param("banco") String banco, @Param("producto") String producto);
}
