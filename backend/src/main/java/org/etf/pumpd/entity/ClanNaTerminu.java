package org.etf.pumpd.entity;

import java.time.LocalDate;

import org.etf.pumpd.converter.StatusPrijaveConverter;
import org.etf.pumpd.enums.*;
import org.hibernate.annotations.Generated;

import jakarta.persistence.*;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * Prijava člana na termin (zahtjev iz 9.2, obrada u 10.12/10.13). idZahtjeva je requestId
 * iz ugovora: baza ga dodjeljuje (AUTO_INCREMENT, nije primarni ključ), a Hibernate ga
 * čita odmah nakon upisa (@Generated).
 */
@Entity
@Table(name="clan_na_terminu")
public class ClanNaTerminu {

	public ClanNaTerminu(Clan clan, Termin termin, StatusPrijave status,
			LocalDate datumZahtjeva) {
		super();
		this.id = new ClanNaTerminuId(termin.getId(),clan.getNalog().getId());
		this.clan = clan;
		this.termin = termin;
		this.status = status;
		this.datumZahtjeva = datumZahtjeva;
	}

	@EmbeddedId
	private ClanNaTerminuId id;
	
	@MapsId("clanId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;
	
	@MapsId("terminId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="termin_idtermin")
	private Termin termin;
	
	@Convert(converter=StatusPrijaveConverter.class)
	@Column(name="status",nullable=false)
	private StatusPrijave status;
	
	@Generated
	@Column(name="idzahtjev",insertable=false,updatable=false)
	private Integer idZahtjeva;
	
	@Column(name="datumZahtjeva")
	private LocalDate datumZahtjeva;
	
	protected ClanNaTerminu()
	{
		
	}

	public void postaviStatus(StatusPrijave status)
	{
		this.status=status;
	}
	public ClanNaTerminuId getId() {
		return id;
	}

	public Clan getClan() {
		return clan;
	}

	public Termin getTermin() {
		return termin;
	}

	public StatusPrijave getStatus() {
		return status;
	}

	public Integer getIdZahtjeva() {
		return idZahtjeva;
	}

	public LocalDate getDatumZahtjeva() {
		return datumZahtjeva;
	}
	

}
