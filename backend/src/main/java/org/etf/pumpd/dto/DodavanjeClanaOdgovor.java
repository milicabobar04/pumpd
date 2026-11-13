package org.etf.pumpd.dto;

/** 11.3 - odgovor 201 Created. temporaryPassword se vraća samo ovdje; korisnik je mora promijeniti. */
public record DodavanjeClanaOdgovor(Integer memberId, String temporaryPassword, String message) {

}
