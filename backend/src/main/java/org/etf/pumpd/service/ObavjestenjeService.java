package org.etf.pumpd.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.entity.*;
import org.etf.pumpd.repository.*;
import org.etf.pumpd.dto.*;


@Service
public class ObavjestenjeService {

	public ObavjestenjeService(ObavjestenjeRepository obavjestenjeRepository, NalogRepository nalogRepository,
			Clock clock) {
		super();
		this.obavjestenjeRepository = obavjestenjeRepository;
		this.nalogRepository = nalogRepository;
		this.clock = clock;
	}

	static final int MAX_DUZINA_SADRZAJA= 1000;
	private final ObavjestenjeRepository obavjestenjeRepository;
	private final NalogRepository nalogRepository;
	private final Clock clock;
	
	
	
	@Transactional
	public void posalji(Integer primalacId, String sadrzaj)
	{
		posalji(primalacId,null,sadrzaj);
	}
	
	@Transactional
	public void posalji(Integer primalacId,Integer posiljalacId, String sadrzaj)
	{
		if(primalacId==null)
		{
			throw new IllegalArgumentException("Primalac obavjestenja je obavezan");
		}
		if(sadrzaj==null|| sadrzaj.isBlank())
		{
			throw new IllegalArgumentException("Sadrzaj obavjestenja je obavezan");
		}
		
		Obavjestenje obavjestenje=new Obavjestenje();
		obavjestenje.setPrimalac(nalogRepository.getReferenceById(primalacId));
		Nalog posiljalac= posiljalacId==null? null: nalogRepository.getReferenceById(posiljalacId);
		obavjestenje.setPosiljalac(posiljalac);
		obavjestenje.setSadrzaj(skrati(sadrzaj.trim()));
		obavjestenje.setDatumVrijeme(LocalDateTime.now(clock));
		obavjestenje.setProcitano(false);
		obavjestenjeRepository.save(obavjestenje);
	}
	
	@Transactional
	public void posaljiSvima(Collection<Integer> primaoci, String sadrzaj)
	{
		Set<Integer> jedinstveni=new LinkedHashSet<>(primaoci);
		for(Integer primalacId:jedinstveni)
		{
			posalji(primalacId,sadrzaj);
		}
	}
	
	@Transactional(readOnly=true)
	public List<ObavjestenjeDto> mojaObavjestenja(Integer korisnikId)
	{
		return obavjestenjeRepository.findByPrimalacIdOrderByDatumVrijemeDescIdDesc(korisnikId).stream().map(o->new ObavjestenjeDto(o.getId(),o.getSadrzaj(),o.getDatumVrijeme(),o.isProcitano())).toList();
		
	}
	
	@Transactional
	public int oznaciSveKaoProcitana(Integer korisnikId)
	{
		return obavjestenjeRepository.oznaciSvaKaoProcitana(korisnikId);
	}
	
	private String skrati(String sadrzaj)
	{
		return sadrzaj.length()<= MAX_DUZINA_SADRZAJA? sadrzaj:sadrzaj.substring(0,MAX_DUZINA_SADRZAJA-3)+"...";
	}
}