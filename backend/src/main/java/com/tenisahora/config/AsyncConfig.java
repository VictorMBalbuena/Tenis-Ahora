package com.tenisahora.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor para el procesamiento asíncrono del webhook de Mercado Pago (CU78): el controller responde 200 antes de
 * procesar, así que el aviso se termina de acreditar en otro hilo. Cola acotada con descarte explícito: un pico de
 * avisos no puede agotar la memoria ni tumbar el proceso (A05); lo que se descarta lo recupera la conciliación.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mp-webhook-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler((tarea, pool) ->
                log.error("Cola de acreditación de pagos llena: se descarta un aviso (lo recupera la conciliación)"));
        executor.initialize();
        return executor;
    }
}
