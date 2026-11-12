package org.etf.pumpd.dto;

import java.time.LocalDate;

import org.etf.pumpd.enums.StatusProblema;

/** Prijavljeni problem za administratora (4.2). */
public record ProblemDto(Integer issueId, String title, String description, StatusProblema status,
		LocalDate reportDate) {

}
