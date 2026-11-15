package org.etf.pumpd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 11.8: isAllowed = false znači da ulazak nije evidentiran (nema aktivnu članarinu). */
public record UlazakOdgovor(Integer memberId, String memberName,
		@JsonProperty("isAllowed") boolean isAllowed, String message) {

}
