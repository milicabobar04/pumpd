package org.etf.pumpd.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClanarinaDto(LocalDate paymentDate, LocalDate validUntil, Double price,@JsonProperty("isActive") boolean isActive) {

}
