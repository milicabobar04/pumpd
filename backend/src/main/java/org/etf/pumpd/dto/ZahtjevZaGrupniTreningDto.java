package org.etf.pumpd.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** 10.12 GET /api/trainer/group-requests - jedan zahtjev na čekanju. sessionName je naziv grupe termina. */
public record ZahtjevZaGrupniTreningDto(
		Integer requestId,
		Integer memberId,
		String memberName,
		Integer sessionId,
		String sessionName,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
		long enrolledCount,
		Integer maxMembers,
		LocalDate requestDate) {
}
