package org.etf.pumpd.dto;

/** Stavka liste trenera za člana (7.1). */
public record TrenerZaClanaDto(Integer trainerId, String name, String surname, Double averageGrade) {

}
