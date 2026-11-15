package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;

import org.etf.pumpd.dto.ClanarinaClanaDto;
import org.etf.pumpd.dto.ClanarinaDto;
import org.etf.pumpd.dto.IzmjenaClanarineZahtjev;
import org.etf.pumpd.dto.PorukaDto;
import org.etf.pumpd.dto.ProduzenjeClanarineOdgovor;
import org.etf.pumpd.dto.ProduzenjeClanarineZahtjev;
import org.etf.pumpd.entity.Clan;
import org.etf.pumpd.entity.Clanarina;
import org.etf.pumpd.exception.ResursNijePronadjenException;
import org.etf.pumpd.repository.ClanRepository;
import org.etf.pumpd.repository.ClanarinaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClanarinaService {

	

	public ClanarinaService(ClanarinaRepository clanarinaRepository, ClanRepository clanRepository,
			QRKodGenerator qrKodGenerator, Clock clock) {
		super();
		this.clanarinaRepository = clanarinaRepository;
		this.clanRepository = clanRepository;
		this.qrKodGenerator = qrKodGenerator;
		this.clock = clock;
	}

	private final ClanarinaRepository clanarinaRepository;
	private final ClanRepository clanRepository;
	private final QRKodGenerator qrKodGenerator;
	private final Clock clock;
	
	@Transactional(readOnly=true)
	public ClanarinaClanaDto mojaClanarina(Integer idClana)
	{
		Clan clan=ucitajAktivnogClana(idClana);
		Clanarina c=clanarinaRepository.findById(idClana).orElseThrow(()->new ResursNijePronadjenException("Nemate clanarinu."));
		return new ClanarinaClanaDto(c.getDatumPocetka(),c.getDatumIsteka(),c.getCijena(),c.jeAktivna(danas()),qrKodGenerator.kaoDataUrl(clan.getQrKod()));
	}
	
	@Transactional(readOnly=true)
	public ClanarinaDto clanarinaClana(Integer memberId)
	{
		ucitajAktivnogClana(memberId);
		Clanarina c=ucitajClanarinu(memberId);
		return new ClanarinaDto(c.getDatumPocetka(),c.getDatumIsteka(),c.getCijena(),c.jeAktivna(danas()));
	}
	
	@Transactional
	public PorukaDto postaviClanarinu(Integer memberId,IzmjenaClanarineZahtjev z)
	{
		ucitajAktivnogClana(memberId);
		if(z.validUntil().isBefore(z.paymentDate()))
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datum isteka ne moze biti prije datuma uplate.");
		}
		clanarinaRepository.findById(memberId).ifPresentOrElse(
				postojeca->postojeca.postavi(z.price(),z.paymentDate(),z.validUntil()),
				()->clanarinaRepository.save(new Clanarina(memberId,z.price(),z.paymentDate(),z.validUntil())));
		
		return new PorukaDto("Clanarina je sacuvana.");
	}
	
	@Transactional
	public ProduzenjeClanarineOdgovor produziClanarinu(Integer memberId, ProduzenjeClanarineZahtjev z)
	{
		ucitajAktivnogClana(memberId);
		LocalDate noviIstek =ucitajClanarinu(memberId).produzi(z.numMonths(),danas());
		return new ProduzenjeClanarineOdgovor(noviIstek,"Clanarina je produzena.");
	}
	
	private Clan ucitajAktivnogClana(Integer memberId)
	{
		return clanRepository.pronadjiAktivnog(memberId).orElseThrow(()->new ResursNijePronadjenException("Clan ne postoji."));
	}
	
	private Clanarina ucitajClanarinu(Integer memberId)
	{
		return clanarinaRepository.findById(memberId).orElseThrow(()->new ResursNijePronadjenException("Clan nema clanarinu."));
	}
	
	private LocalDate danas()
	{
		return LocalDate.now(clock);
	}
}
