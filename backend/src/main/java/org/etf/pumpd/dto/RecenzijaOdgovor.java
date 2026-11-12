package org.etf.pumpd.dto;

/** 7.5 - odgovor 201 Created; averageGrade je novi prosjek trenera. */
public record RecenzijaOdgovor(Integer reviewId, Double averageGrade, String message) {

}
