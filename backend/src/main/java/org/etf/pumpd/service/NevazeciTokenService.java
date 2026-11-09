package org.etf.pumpd.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.etf.pumpd.entity.NevazeciToken;
import org.etf.pumpd.repository.NevazeciTokenRepository;

@Service
public class NevazeciTokenService {

	private final NevazeciTokenRepository repository;
	private final Clock clock;

	public NevazeciTokenService(NevazeciTokenRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional
	public void ponisti(String jti, Instant istice)
	{
		if (!repository.existsById(jti)) {
			repository.save(new NevazeciToken(jti, istice));
		}
	}

	@Transactional(readOnly = true)
	public boolean jePonisten(String jti)
	{
		return repository.existsById(jti);
	}

	/** Briše tokene kojima je rok važenja ionako prošao (svakih sat vremena). */
	@Scheduled(fixedRateString = "PT1H")
	@Transactional
	public int obrisiIstekle()
	{
		return repository.obrisiIstekle(clock.instant());
	}
}
