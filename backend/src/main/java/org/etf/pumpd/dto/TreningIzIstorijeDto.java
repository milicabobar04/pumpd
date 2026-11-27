package org.etf.pumpd.dto;

import java.time.LocalDate;

import org.etf.pumpd.enums.VrstaTreninga;

/**
 * 2.4 GET /api/users/training-history - jedan trening. trainingId je id termina (GROUP) ili id
 * dolaska (SOLO), pa je jedinstven tek zajedno sa type.
 */
public record TreningIzIstorijeDto(Integer trainingId, String name, LocalDate date, VrstaTreninga type) {
}
