package org.etf.pumpd.service;

import java.util.Optional;

import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.repository.*;
import org.etf.pumpd.enums.*;

@Service
public class KorisnikService {

	public KorisnikService(NalogRepository nalogRepository) {
		super();
		this.nalogRepository = nalogRepository;
	}

	private final NalogRepository nalogRepository;
	
	@Transactional(readOnly=true)
	public Optional<Nalog> pronadjiPoEmailu(String email)
	{
		return nalogRepository.findByEmail(email);
	}
	
	@Transactional(readOnly = true)
	 public boolean emailPostoji(String email) 
	{
	        return nalogRepository.existsByEmail(email);
	}
	
	@Transactional(readOnly = true)
    public Optional<Uloga> odrediUlogu(Integer idNalog)
	{
        return nalogRepository.pronadjiUlogu(idNalog).map(Uloga::valueOf);
    }
	
}
