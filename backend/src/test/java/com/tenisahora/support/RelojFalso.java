package com.tenisahora.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj que se mueve a mano: permite probar bloqueos de 15 minutos sin esperar 15 minutos. */
public class RelojFalso extends Clock {

    private Instant ahora;

    public RelojFalso(Instant inicio) {
        this.ahora = inicio;
    }

    public void avanzar(Duration tiempo) {
        ahora = ahora.plus(tiempo);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
