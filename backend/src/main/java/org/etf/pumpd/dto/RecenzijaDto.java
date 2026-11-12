package org.etf.pumpd.dto;

import java.time.LocalDate;

/** Recenzija u odgovorima 7.2 i 7.6. memberName = "Ime Prezime". */
public record RecenzijaDto(Integer reviewId, String memberName, Integer grade, String comment, LocalDate date) {

}
