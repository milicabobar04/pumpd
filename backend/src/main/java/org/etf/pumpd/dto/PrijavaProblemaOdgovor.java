package org.etf.pumpd.dto;

import org.etf.pumpd.enums.*;

public record PrijavaProblemaOdgovor(Integer issueId, StatusProblema status, String message) {

}
