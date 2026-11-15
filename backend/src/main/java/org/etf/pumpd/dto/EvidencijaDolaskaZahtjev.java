package org.etf.pumpd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 11.8 i 11.9: tijelo zahtjeva, QR kod sa kartice člana. */
public record EvidencijaDolaskaZahtjev(
		@NotBlank(message = "QR kod je obavezan.")
		@Size(max = 64, message = "QR kod moze imati najvise 64 znaka.")
		String qrCode) {

}
