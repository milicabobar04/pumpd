package org.etf.pumpd.dto;

import java.time.LocalDate;

public record InfoTeretanaDto(String name,String description,Double membershipPrice,LocalDate dateOfEstablishment,String workingHours) {

}
