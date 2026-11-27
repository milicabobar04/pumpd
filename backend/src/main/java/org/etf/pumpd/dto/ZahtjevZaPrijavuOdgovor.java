package org.etf.pumpd.dto;

import org.etf.pumpd.enums.StatusPrijaveClana;

/** 9.2 POST /api/group-trainings/{sessionId}/enrollment - odgovor 201 (status je uvijek PENDING). */
public record ZahtjevZaPrijavuOdgovor(Integer requestId, StatusPrijaveClana status, String message) {
}
