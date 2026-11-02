package org.etf.pumpd.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.function.Executable;
import org.springframework.web.server.ResponseStatusException;

/** Zajednički pomoćni dio za testove servisa. */
final class TestPomocni {

    /** Fiksirano "sada": 29.10.2026. u 10:15 UTC. */
    static final Clock SAT = Clock.fixed(Instant.parse("2026-10-29T10:15:00Z"), ZoneOffset.UTC);

    private TestPomocni() {
    }

    /** Provjerava da kod baca ResponseStatusException sa traženim HTTP statusom. */
    static ResponseStatusException assertStatus(int ocekivaniStatus, Executable kod) {
        ResponseStatusException izuzetak = assertThrows(ResponseStatusException.class, kod);
        assertEquals(ocekivaniStatus, izuzetak.getStatusCode().value());
        return izuzetak;
    }
}
