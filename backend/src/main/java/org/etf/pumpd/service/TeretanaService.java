package org.etf.pumpd.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.repository.*;
import org.etf.pumpd.entity.*;

@Service
public class TeretanaService {

	
	public TeretanaService(TeretanaRepository teretanaRepository) {
		super();
		this.teretanaRepository = teretanaRepository;
	}

	private final TeretanaRepository teretanaRepository;
	
	@Transactional(readOnly = true)
    public InfoTeretanaDto informacije() {
        Teretana teretana = teretanaRepository.findById(Teretana.ID).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Informacije o teretani nisu unesene."));
        BigDecimal cijena = teretana.getCijenaClanarine();
        return new InfoTeretanaDto(
                teretana.getNaziv(),
                teretana.getOpis(),
                cijena == null ? null : cijena.doubleValue(),
                teretana.getDatumOsnivanja(),
                teretana.getRadnoVrijeme());
    }
}