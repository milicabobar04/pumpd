package org.etf.pumpd.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 1.1 POST /api/auth/login - tijelo zahtjeva. */
public record PrijavaZahtjev(
		@NotBlank(message = "Email je obavezan.")
		@Email(message = "Email nije ispravnog formata.")
		String email,

		@NotBlank(message = "Lozinka je obavezna.")
		String password) {

}
