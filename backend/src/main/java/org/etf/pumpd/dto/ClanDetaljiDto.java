package org.etf.pumpd.dto;

import java.time.LocalDate;

/** Detalji člana za zaposlene (11.2). */
public record ClanDetaljiDto(Integer memberId, String name, String surname, String email,
		Integer birthYear, Double weight, Double height,
		Boolean membershipActive, LocalDate membershipValidUntil) {

}
