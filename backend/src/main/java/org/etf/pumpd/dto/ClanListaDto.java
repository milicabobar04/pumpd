package org.etf.pumpd.dto;

/** Stavka liste članova za zaposlene (11.1). */
public record ClanListaDto(Integer memberId, String name, String surname, String email, Boolean membershipActive) {

}
