package org.etf.pumpd.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

/** Recenzija koju član ostavlja treneru (7.5, 7.2, 7.6). Ocjena je 1–5 (CHECK u bazi, V2). */
@Entity
@Table(name="recenzija")
public class Recenzija {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idrecenzija")
	private Integer id;

	/** U bazi smije biti NULL (stari podaci); nove recenzije uvijek imaju ocjenu. */
	@Column( name="ocjena")
	private Integer ocjena;

	@Column( name="komentar",length=500)
	private String komentar;

	@Column( name="datum")
	private LocalDate datum;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="trener_nalog_idnalog")
	private Trener trener;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;

	protected Recenzija()
	{

	}

	public Recenzija(Trener trener, Clan clan, Integer ocjena, String komentar, LocalDate datum) {
		this.trener = trener;
		this.clan = clan;
		this.ocjena = ocjena;
		this.komentar = komentar;
		this.datum = datum;
	}

	/** 7.5: ponovna recenzija istog člana istom treneru zamjenjuje staru. */
	public void izmijeni(Integer ocjena, String komentar, LocalDate datum) {
		this.ocjena = ocjena;
		this.komentar = komentar;
		this.datum = datum;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getOcjena() {
		return ocjena;
	}

	public void setOcjena(Integer ocjena) {
		this.ocjena = ocjena;
	}

	public String getKomentar() {
		return komentar;
	}

	public void setKomentar(String komentar) {
		this.komentar = komentar;
	}

	public LocalDate getDatum() {
		return datum;
	}

	public void setDatum(LocalDate datum) {
		this.datum = datum;
	}

	public Trener getTrener() {
		return trener;
	}

	public Clan getClan() {
		return clan;
	}
}
