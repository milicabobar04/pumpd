package org.etf.pumpd.dto;

/** Detalji trenera za administratora (12.2). */
public record TrenerDetaljiDto(Integer trainerId, String name, String surname, String email,
		String biography, Double averageGrade, Integer followerCount) {

}
