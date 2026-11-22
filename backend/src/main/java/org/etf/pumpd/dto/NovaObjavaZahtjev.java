package org.etf.pumpd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 8.6 POST /api/posts - tijelo zahtjeva. imageUrl je opcion; ako je poslat, mora biti adresa
 * slike koju je trener prethodno poslao (POST /api/posts/images), što provjerava servis.
 */
public record NovaObjavaZahtjev(
		@NotBlank(message = "Sadrzaj objave je obavezan.")
		@Size(max = 500, message = "Sadrzaj objave moze imati najvise 500 znakova.")
		String content,

		@Size(max = 255, message = "Adresa slike moze imati najvise 255 znakova.")
		String imageUrl) {

}
