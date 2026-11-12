package org.etf.pumpd.entity;

import java.time.LocalDate;

import org.etf.pumpd.converter.*;
import org.etf.pumpd.enums.*;

import jakarta.persistence.*;

/**
 * Praćenje trenera i zahtjev za praćenje (7.3, 7.4, 10.1–10.3).
 * Primarni ključ u bazi je par (trener, član), pa član ima najviše jedan red po treneru.
 * U entitetu je @Id kolona idpracenje (AUTO_INCREMENT, UNIQUE): to je requestId iz ugovora.
 */
@Entity
@Table(name="pracenje")
public class Pracenje {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idpracenje")
	private Integer id;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="trener_nalog_idnalog")
	private Trener trener;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;

	@Column( name="DatumPracenja")
	private LocalDate datum;

	@Convert(converter=StatusPracenjaConverter.class)
	@Column( name="status",nullable=false)
	private StatusPracenja status;

	protected Pracenje()
	{

	}

	public Pracenje(Trener trener, Clan clan, LocalDate datum, StatusPracenja status) {
		this.trener = trener;
		this.clan = clan;
		this.datum = datum;
		this.status = status;
	}

	/** 7.3: ponovni zahtjev nakon odbijanja koristi isti red (par trener–član je primarni ključ). */
	public void posaljiPonovo(LocalDate datum) {
		this.status = StatusPracenja.PENDING;
		this.datum = datum;
	}

	/** 10.2: trener prihvata zahtjev, član sada prati trenera. */
	public void prihvati() {
		this.status = StatusPracenja.ACCEPTED;
	}

	/** 10.2: trener odbija zahtjev; red ostaje, pa član može poslati novi (7.3). */
	public void odbij() {
		this.status = StatusPracenja.REJECTED;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Trener getTrener() {
		return trener;
	}

	public Clan getClan() {
		return clan;
	}

	public LocalDate getDatum() {
		return datum;
	}

	public void setDatum(LocalDate datum) {
		this.datum = datum;
	}

	public StatusPracenja getStatus() {
		return status;
	}

	public void setStatus(StatusPracenja status) {
		this.status = status;
	}
}
