package org.etf.pumpd.dto;

import java.time.LocalDate;

/** Zahtjev za praćenje koji čeka trenera (10.1). memberName = "Ime Prezime". */
public record ZahtjevZaPracenjeDto(Integer requestId, Integer memberId, String memberName, LocalDate requestDate) {

}
