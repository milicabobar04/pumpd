package org.etf.pumpd.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 7.5 POST /api/trainers/{trainerId}/reviews - tijelo zahtjeva. Komentar je opcion. */
public record RecenzijaZahtjev(
		@NotNull(message = "Ocjena je obavezna.")
		@Min(value = 1, message = "Ocjena mora biti izmedju 1 i 5.")
		@Max(value = 5, message = "Ocjena mora biti izmedju 1 i 5.")
		Integer grade,

		@Size(max = 500, message = "Komentar moze imati najvise 500 znakova.")
		String comment) {

}
