package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** 11.9: odgovor na evidentiran izlazak. */
public record IzlazakOdgovor(Integer memberId, String memberName,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime exitTime, String message) {

}
