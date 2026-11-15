package org.etf.pumpd.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClanarinaClanaDto(LocalDate paymentDate, LocalDate validUntil, Double price,@JsonProperty("isActive") boolean isActive,String qrCodeUrl) {

}
