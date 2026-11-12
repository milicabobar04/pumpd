package org.etf.pumpd.dto;

import jakarta.validation.constraints.NotNull;

import org.etf.pumpd.enums.OdlukaOZahtjevu;

/** 10.2 PATCH /api/trainer/follow-requests/{requestId} - tijelo zahtjeva. */
public record ObradaZahtjevaZahtjev(
		@NotNull(message = "Status je obavezan.")
		OdlukaOZahtjevu status) {

}
