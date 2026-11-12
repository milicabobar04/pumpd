package org.etf.pumpd.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 5.2 PUT /api/gym/info - tijelo zahtjeva. Sva polja su obavezna (ugovor ih ne označava kao null).
 * Da datum osnivanja nije u budućnosti provjerava servis, jer zavisi od današnjeg datuma.
 */
public record IzmjenaTeretaneZahtjev(
		@NotBlank(message = "Naziv je obavezan.")
		@Size(max = 45, message = "Naziv moze imati najvise 45 znakova.")
		String name,

		@NotBlank(message = "Opis je obavezan.")
		@Size(max = 500, message = "Opis moze imati najvise 500 znakova.")
		String description,

		@NotNull(message = "Cijena clanarine je obavezna.")
		@Positive(message = "Cijena clanarine mora biti veca od 0.")
		Double membershipPrice,

		@NotNull(message = "Datum osnivanja je obavezan.")
		LocalDate dateOfEstablishment,

		@NotBlank(message = "Radno vrijeme je obavezno.")
		@Size(max = 45, message = "Radno vrijeme moze imati najvise 45 znakova.")
		String workingHours) {

}
