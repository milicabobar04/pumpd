package org.etf.pumpd.dto;

import org.etf.pumpd.enums.StatusPracenjaClana;

/** 7.3 - odgovor 201 Created. status je uvijek PENDING. */
public record ZahtjevZaPracenjeOdgovor(Integer followRequestId, StatusPracenjaClana status, String message) {

}
