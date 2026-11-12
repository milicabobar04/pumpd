package org.etf.pumpd.dto;

import jakarta.validation.constraints.NotNull;

import org.etf.pumpd.enums.StatusProblema;

/** 4.3 PATCH /api/admin/issues/{issueId} - tijelo zahtjeva. */
public record PromjenaStatusaProblemaZahtjev(
		@NotNull(message = "Status je obavezan.")
		StatusProblema status) {

}
