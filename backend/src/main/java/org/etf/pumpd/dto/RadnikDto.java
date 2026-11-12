package org.etf.pumpd.dto;

/** Radnik za administratora: stavka liste (12.5) i detalji (12.6) imaju ista polja. */
public record RadnikDto(Integer workerId, String name, String surname, String email) {

}
