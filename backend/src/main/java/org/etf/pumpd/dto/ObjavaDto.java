package org.etf.pumpd.dto;

import java.time.LocalDateTime;

/**
 * Objava u feedu (8.1) i u sačuvanim objavama (8.3). trainerName = "Ime Prezime".
 * Record zadržava ime polja, pa je u JSON-u ključ "isSaved" (kao u ugovoru), a ne "saved".
 */
public record ObjavaDto(Integer postId, Integer trainerId, String trainerName, String content, String imageUrl,
		LocalDateTime postTimestamp, boolean isSaved) {

}
