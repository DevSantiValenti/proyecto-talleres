package com.analistas.lavadero.model.domain;

import java.time.*;

public class RelojPrueba extends Clock {
    private Instant ahora = Instant.parse("2026-10-07T15:00:00Z");

    public void fijar(Instant instant) {
        ahora = instant;
    }

    @Override
    public Instant instant() {
        return ahora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("America/Argentina/Cordoba");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(ahora, zone);
    }
}
