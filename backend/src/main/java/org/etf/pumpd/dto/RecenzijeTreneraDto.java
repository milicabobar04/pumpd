package org.etf.pumpd.dto;

import java.util.List;

/** 7.6 - recenzije prijavljenog trenera. */
public record RecenzijeTreneraDto(Double averageGrade, List<RecenzijaDto> reviews) {

}
