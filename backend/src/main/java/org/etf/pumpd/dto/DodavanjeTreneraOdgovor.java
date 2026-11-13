package org.etf.pumpd.dto;

/** 12.3 - odgovor 201 Created. temporaryPassword se vraća samo ovdje; korisnik je mora promijeniti. */
public record DodavanjeTreneraOdgovor(Integer trainerId, String temporaryPassword, String message) {

}
