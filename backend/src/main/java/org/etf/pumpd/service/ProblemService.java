package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDateTime;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import org.etf.pumpd.dto.*;
import org.etf.pumpd.repository.*;
import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.*;



@Service
public class ProblemService {

	

	public ProblemService(ProblemRepository problemRepository, NalogRepository nalogRepository, Clock clock) {
		super();
		this.problemRepository = problemRepository;
		this.nalogRepository = nalogRepository;
		this.clock = clock;
	}

	static final int MAX_DUZINA_NASLOVA= 150;
	static final int MAX_DUZINA_OPISA= 500;
	private final ProblemRepository problemRepository;
	private final NalogRepository nalogRepository;
	private final Clock clock;
	
	
	
	@Transactional
	public PrijavaProblemaOdgovor prijavi(Integer korisnikId,PrijavaProblemaZahtjev zahtjev)
	{
		String naslov= obavezno(zahtjev.title(), "Naslov",MAX_DUZINA_NASLOVA);
		String opis= obavezno(zahtjev.description(), "Opis",MAX_DUZINA_OPISA);	
		
		Problem problem=new Problem();
		problem.setNaslov(naslov);
	    problem.setOpis(opis);
	    problem.setStatus(StatusProblema.UNPROCESSED);
	    problem.setVrijeme(LocalDateTime.now(clock));
	    problem.setPrijavio(pronadjiPrijavio(korisnikId));

	    Problem sacuvan = problemRepository.save(problem);
	    return new PrijavaProblemaOdgovor(sacuvan.getId(), sacuvan.getStatus(),"Problem je uspjesno prijavljen");
	}
	
	
	
	
	private Nalog pronadjiPrijavio(Integer korisnikId)
	{
		if(korisnikId==null)
		{
			return null;
		}
		return nalogRepository.findById(korisnikId).orElse(null);
	}
	
	private String obavezno(String vrijednost, String naziv,int maxDuzina)
	{
		String ocisceno= vrijednost==null?"": vrijednost.trim();
		if(ocisceno.isEmpty())
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,naziv+" je obavezan");
			
		}
		if(ocisceno.length()>maxDuzina)
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,naziv+" moze imati najvise "+maxDuzina+" znakova");
			
		}
		return ocisceno;
	}
}