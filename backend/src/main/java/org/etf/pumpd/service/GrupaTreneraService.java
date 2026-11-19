package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.etf.pumpd.dto.*;
import org.etf.pumpd.entity.*;
import org.etf.pumpd.enums.StatusPracenja;
import org.etf.pumpd.enums.StatusPrijave;
import org.etf.pumpd.exception.KonfliktException;
import org.etf.pumpd.exception.ResursNijePronadjenException;

import java.util.*;
import java.util.stream.Stream;

import org.etf.pumpd.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GrupaTreneraService {

	public GrupaTreneraService(TerminRepository terminRepository, GrupaRepository grupaRepository,
			ClanUGrupiRepository clanUGrupiRepository, ClanNaTerminuRepository prijavaRepository,
			ClanRepository clanRepository, PracenjeRepository pracenjeRepository,
			ObavjestenjeService obavjestenjeService, Clock clock) {
		super();
		this.terminRepository = terminRepository;
		this.grupaRepository = grupaRepository;
		this.clanUGrupiRepository = clanUGrupiRepository;
		this.prijavaRepository = prijavaRepository;
		this.clanRepository = clanRepository;
		this.pracenjeRepository = pracenjeRepository;
		this.obavjestenjeService = obavjestenjeService;
		this.clock = clock;
	}

	private static final DateTimeFormatter FORMAT_VREMENA = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

	private final TerminRepository terminRepository;
	private final GrupaRepository grupaRepository;
	private final ClanUGrupiRepository clanUGrupiRepository;
	private final ClanNaTerminuRepository prijavaRepository;
	private final ClanRepository clanRepository;
	private final PracenjeRepository pracenjeRepository;
	private final ObavjestenjeService obavjestenjeService;
	private final Clock clock;
	
	@Transactional(readOnly=true)
	public List<GrupaDto> listaGrupa(Integer idTrenera)
	{
		LocalDateTime sada=sada();
		List<Termin> termini=terminRepository.terminiTrenera(idTrenera);
		Stream<Termin> predstojeci=termini.stream().filter(t->!t.jePoceo(sada)).sorted(Comparator.comparing(Termin::getVrijeme));
		Stream<Termin> prosli=termini.stream().filter(t->t.jePoceo(sada)).sorted(Comparator.comparing(Termin::getVrijeme).reversed());
		return Stream.concat(predstojeci,prosli).map(t->new GrupaDto(t.getGrupa().getId(),t.getId(),t.getGrupa().getNaziv(),t.getVrijeme(),clanUGrupiRepository.brojClanova(t.getGrupa().getId()),t.getGrupa().getMaxClanova())).toList();
	}
	
	@Transactional(readOnly=true)
	public List<ClanGrupeDto> clanoviGrupe(Integer idTrenera,Integer idGrupe)
	{
		ucitajSvojTermin(idTrenera,idGrupe);
		return clanUGrupiRepository.clanoviGrupe(idGrupe).stream().map(cg->{
			Nalog nalog =cg.getClan().getNalog();
			return new ClanGrupeDto(nalog.getId(),nalog.getIme(),nalog.getPrezime());
		}).toList();
	}
	
	// READ_COMMITTED: nakon čekanja na zaključanu grupu, brojanje članova mora vidjeti
	// članove koje je u međuvremenu upisala druga transakcija (MySQL podrazumijeva REPEATABLE READ,
	// koji bi vratio stari broj, pa bi dva istovremena dodavanja zauzela isto posljednje mjesto).
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public PorukaDto dodajClana(Integer idTrenera,Integer idGrupe,Integer idClana)
	{
		Termin termin=ucitajSvojTermin(idTrenera,idGrupe);
		provjeriDaNijePoceo(termin);
		Clan clan=clanRepository.pronadjiAktivnog(idClana).orElseThrow(()->new ResursNijePronadjenException("Clan ne postoji."));
		boolean pratilac=pracenjeRepository.findByTrenerIdAndClanId(idTrenera,idClana).filter(p->p.getStatus()==StatusPracenja.ACCEPTED).isPresent();
		if(!pratilac)
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Clan vas ne prati, u grupu se mogu dodati samo pratioci.");
		}
		Grupa grupa=grupaRepository.zakljucaj(idGrupe).orElseThrow();
		if(clanUGrupiRepository.existsById(new ClanUGrupiId(idClana,idGrupe)))
		{
			throw new KonfliktException("Clan je vec u grupi.");
		}
		if(clanUGrupiRepository.brojClanova(idGrupe)>= grupa.getMaxClanova())
		{
			throw new KonfliktException("Grupa je puna.");
		}
		LocalDate danas=LocalDate.now(clock);
		clanUGrupiRepository.save(new ClanUGrupi(clan,grupa,danas));
		prijavaRepository.findById(new ClanNaTerminuId(termin.getId(),idClana)).ifPresentOrElse(
				prijava->prijava.postaviStatus(StatusPrijave.PRIJAVLJEN),
				()->prijavaRepository.save(new ClanNaTerminu(clan,termin,StatusPrijave.PRIJAVLJEN,danas)));
		obavjestenjeService.posalji(idClana,idTrenera,"Trener vas je dodao u grupu \""+grupa.getNaziv()+"\"(termin " + FORMAT_VREMENA.format(termin.getVrijeme())+").");
		return new PorukaDto("Clan je dodat u grupu.");
	}
	
	@Transactional
	public PorukaDto ukloniClana(Integer idTrenera,Integer idGrupe,Integer idClana)
	{
		Termin termin=ucitajSvojTermin(idTrenera,idGrupe);
		provjeriDaNijePoceo(termin);
		
		ClanUGrupi clanUGrupi= clanUGrupiRepository.findById(new ClanUGrupiId(idClana,idGrupe)).orElseThrow(()->new ResursNijePronadjenException("Clan nije u ovoj grupi."));
		clanUGrupiRepository.delete(clanUGrupi);

		prijavaRepository.findById(new ClanNaTerminuId(termin.getId(),idClana)).ifPresent(prijavaRepository::delete);
		obavjestenjeService.posalji(idClana,idTrenera,"Trener vas je uklonio iz grupe \""+termin.getGrupa().getNaziv()+"\"(termin " + FORMAT_VREMENA.format(termin.getVrijeme())+").");
		return new PorukaDto("Clan je uklonjen iz grupe.");
	}
	
	private Termin ucitajSvojTermin(Integer idTrenera,Integer idGrupe)
	{
		Termin termin=terminRepository.poGrupi(idGrupe).orElseThrow(()->new ResursNijePronadjenException("Grupa ne postoji."));
		if(!termin.pripadaTreneru(idTrenera))
		{
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Nemate pristup ovoj grupi.");
		}
		return termin;
	}
	
	private void provjeriDaNijePoceo(Termin termin)
	{
		if(termin.jePoceo(sada()))
		{
			throw new KonfliktException("Termin je vec poceo. Clanovi grupe se vise ne mogu mijenjati.");
		}
	}
	
	private LocalDateTime sada()
	{
		return LocalDateTime.now(clock);
	}
}
