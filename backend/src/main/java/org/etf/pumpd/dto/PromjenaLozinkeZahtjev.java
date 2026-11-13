package org.etf.pumpd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PUT /api/users/my-password - tijelo zahtjeva. Nova lozinka ima ista pravila kao pri dodavanju naloga. */
public record PromjenaLozinkeZahtjev(
		@NotBlank(message = "Stara lozinka je obavezna.")
		String oldPassword,

		@NotBlank(message = "Nova lozinka je obavezna.")
		@Size(min = 8, max = 72, message = "Nova lozinka mora imati izmedju 8 i 72 znaka.")
		String newPassword) {

}
