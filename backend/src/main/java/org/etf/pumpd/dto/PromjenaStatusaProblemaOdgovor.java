package org.etf.pumpd.dto;

import org.etf.pumpd.enums.StatusProblema;

/** 4.3 - odgovor 200 OK. */
public record PromjenaStatusaProblemaOdgovor(Integer issueId, StatusProblema status, String message) {

}
