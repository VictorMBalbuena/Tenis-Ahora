package com.tenisahora.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Las rondas siguientes del cuadro se crean sin participantes hasta que alguien gana, pero las bases creadas antes
 * tienen esas columnas como NOT NULL y ddl-auto no lo corrige. Sin esto, generar un cuadro de 4 o más inscriptos falla.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AjusteDeEsquemaDePartidos implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE partido ALTER COLUMN participante_a_id DROP NOT NULL");
            jdbc.execute("ALTER TABLE partido ALTER COLUMN participante_b_id DROP NOT NULL");
        } catch (DataAccessException e) {
            log.warn("No se pudo ajustar la tabla de partidos: {}", e.getMessage());
        }
    }
}
