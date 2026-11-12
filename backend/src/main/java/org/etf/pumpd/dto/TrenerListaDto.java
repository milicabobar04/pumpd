package org.etf.pumpd.dto;

/** Stavka liste trenera za administratora (12.1). */
public record TrenerListaDto(Integer trainerId, String name, String surname, String email) {

}
