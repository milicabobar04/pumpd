package org.etf.pumpd.dto;

import jakarta.validation.constraints.*;

public record DodavanjeUGrupuZahtjev(
		@NotNull(message="Clan je obavezan.")
		@Positive(message="Id clana mora biti pozitivan broj.")
		Integer memberId) {

}
