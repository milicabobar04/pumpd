package org.etf.pumpd.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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

	
	public TeretanaService(TeretanaRepository teretanaRepository, Clock clock) {
		super();
		this.teretanaRepository = teretanaRepository;
		this.clock = clock;
	}

	private final TeretanaRepository teretanaRepository;
	private final Clock clock;
	
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

	/**
	 * 5.2: admin mijenja informacije o teretani (red idteretana = 1).
	 * Ako red ne postoji, pravi se. Kolone koje ugovor ne koristi (adresa, vlasnik,
	 * kontakt, kapacitet) se ne mijenjaju.
	 */
	@Transactional
	public PorukaDto izmijeni(IzmjenaTeretaneZahtjev zahtjev) {
		if (zahtjev.dateOfEstablishment().isAfter(LocalDate.now(clock))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datum osnivanja ne moze biti u buducnosti.");
		}
		Teretana teretana = teretanaRepository.findById(Teretana.ID).orElseGet(Teretana::new);
		teretana.setNaziv(zahtjev.name().trim());
		teretana.setOpis(zahtjev.description().trim());
		teretana.setCijenaClanarine(BigDecimal.valueOf(zahtjev.membershipPrice()));
		teretana.setDatumOsnivanja(zahtjev.dateOfEstablishment());
		teretana.setRadnoVrijeme(zahtjev.workingHours().trim());
		teretanaRepository.save(teretana);
		return new PorukaDto("Informacije o teretani su azurirane.");
	}
}
