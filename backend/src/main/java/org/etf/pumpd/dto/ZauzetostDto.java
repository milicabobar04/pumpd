package org.etf.pumpd.dto;

/** 5.3: broj članova trenutno u teretani i kapacitet (null ako nije unesen). */
public record ZauzetostDto(long currentMembers, Integer capacity) {

}
