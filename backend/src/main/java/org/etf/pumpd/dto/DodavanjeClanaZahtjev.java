package org.etf.pumpd.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 11.3 POST /api/worker/members - tijelo zahtjeva. Lozinku generiše server (temporaryPassword u odgovoru). Uloga je uvijek MEMBER i ne šalje se.
 * Gornja granica za birthYear (tekuća godina) provjerava se u servisu jer zavisi od datuma.
 */
public record DodavanjeClanaZahtjev(
		@NotBlank(message = "Ime je obavezno.")
		@Size(max = 45, message = "Ime moze imati najvise 45 znakova.")
		String name,

		@NotBlank(message = "Prezime je obavezno.")
		@Size(max = 45, message = "Prezime moze imati najvise 45 znakova.")
		String surname,

		@NotBlank(message = "Email je obavezan.")
		@Email(message = "Email nije ispravnog formata.")
		@Size(max = 100, message = "Email moze imati najvise 100 znakova.")
		String email,

		@Min(value = 1900, message = "Godina rodjenja ne moze biti prije 1900.")
		Integer birthYear,

		@Positive(message = "Tezina mora biti veca od 0.")
		@DecimalMax(value = "500", message = "Tezina moze biti najvise 500 kg.")
		Double weight,

		@Positive(message = "Visina mora biti veca od 0.")
		@DecimalMax(value = "300", message = "Visina moze biti najvise 300 cm.")
		Double height) {

}
