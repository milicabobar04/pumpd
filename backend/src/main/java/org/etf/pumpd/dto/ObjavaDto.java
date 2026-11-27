package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Objava u feedu (8.1) i u sačuvanim objavama (8.3). trainerName = "Ime Prezime".
 * Record zadržava ime polja, pa je u JSON-u ključ "isSaved" (kao u ugovoru), a ne "saved".
 * postTimestamp u obliku iz ugovora (YYYY-MM-DDTHH:mm:ss), zadat izričito kao i na ostalim rutama.
 */
public record ObjavaDto(Integer postId, Integer trainerId, String trainerName, String content, String imageUrl,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime postTimestamp, boolean isSaved) {

}
