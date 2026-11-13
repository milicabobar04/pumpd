package org.etf.pumpd.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 12.7 POST /api/admin/workers - tijelo zahtjeva. Lozinku generiše server (temporaryPassword u odgovoru). */
public record DodavanjeRadnikaZahtjev(
		@NotBlank(message = "Ime je obavezno.")
		@Size(max = 45, message = "Ime moze imati najvise 45 znakova.")
		String name,

		@NotBlank(message = "Prezime je obavezno.")
		@Size(max = 45, message = "Prezime moze imati najvise 45 znakova.")
		String surname,

		@NotBlank(message = "Email je obavezan.")
		@Email(message = "Email nije ispravnog formata.")
		@Size(max = 100, message = "Email moze imati najvise 100 znakova.")
		String email) {

}
