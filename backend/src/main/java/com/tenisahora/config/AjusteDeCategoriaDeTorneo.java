package com.tenisahora.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cuando un enum gana un valor, ddl-auto=update no toca el CHECK que creó la primera vez: la base
 * sigue rechazando el valor nuevo aunque el código ya lo tenga. Ya pasó con los tipos de descuento
 * y vuelve a pasar con la categoría MIXTO, que sin esto no se puede guardar en una base existente.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AjusteDeCategoriaDeTorneo implements CommandLineRunner {

    private static final String RESTRICCION = "torneo_categoria_check";

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE torneo DROP CONSTRAINT IF EXISTS " + RESTRICCION);
            jdbc.execute("ALTER TABLE torneo ADD CONSTRAINT " + RESTRICCION
                    + " CHECK (categoria IN ('MASCULINO', 'FEMENINO', 'MIXTO'))");
        } catch (DataAccessException e) {
            log.warn("No se pudo ajustar la categoria de los torneos: {}", e.getMessage());
        }
    }
}
