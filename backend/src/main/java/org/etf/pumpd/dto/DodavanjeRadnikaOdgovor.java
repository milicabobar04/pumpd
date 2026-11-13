package org.etf.pumpd.dto;

/** 12.7 - odgovor 201 Created. temporaryPassword se vraća samo ovdje; korisnik je mora promijeniti. */
public record DodavanjeRadnikaOdgovor(Integer workerId, String temporaryPassword, String message) {

}
