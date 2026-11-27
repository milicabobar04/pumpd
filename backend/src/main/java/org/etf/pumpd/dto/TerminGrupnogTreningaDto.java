package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import org.etf.pumpd.enums.StatusPrijaveClana;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 9.1 GET /api/group-trainings - jedan termin. name je naziv grupe termina, enrolledCount broj
 * članova grupe (kao u 10.4 i 10.8). enrollmentStatus postoji samo za člana; radniku se ne šalje.
 */
public record TerminGrupnogTreningaDto(
		Integer sessionId,
		Integer groupId,
		String name,
		Integer trainerId,
		String trainerName,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
		Integer durationMinutes,
		long enrolledCount,
		Integer maxMembers,
		@JsonInclude(JsonInclude.Include.NON_NULL) StatusPrijaveClana enrollmentStatus) {
}
