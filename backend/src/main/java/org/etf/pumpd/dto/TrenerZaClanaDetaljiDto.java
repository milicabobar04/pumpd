package org.etf.pumpd.dto;

import java.util.List;

import org.etf.pumpd.enums.StatusPracenjaClana;

/** Detalji trenera za člana (7.2). */
public record TrenerZaClanaDetaljiDto(Integer trainerId, String name, String surname, String biography,
		Double averageGrade, StatusPracenjaClana followStatus, List<RecenzijaDto> reviews) {

}
