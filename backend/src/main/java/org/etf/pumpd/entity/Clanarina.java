package org.etf.pumpd.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

/**
 * Članarina člana. Primarni ključ je id člana, pa član ima najviše jednu članarinu.
 * Mapiranje na ugovor: datumPocetka = paymentDate, datumIsteka = validUntil, cijena = price.
 */
@Entity
@Table(name="clanarina")
public class Clanarina {

	@Id
	@Column( name="clan_nalog_idnalog")
	private Integer id;

	@Column( name="cijena", nullable=false)
	private Double cijena;

	@Column( name="datumPocetka", nullable=false)
	private LocalDate datumPocetka;

	@Column( name="datumIsteka", nullable=false)
	private LocalDate datumIsteka;

	protected Clanarina()
	{

	}

	public Clanarina(Integer idClana, Double cijena, LocalDate datumPocetka, LocalDate datumIsteka) {
		this.id = idClana;
		this.cijena = cijena;
		this.datumPocetka = datumPocetka;
		this.datumIsteka = datumIsteka;
	}

	/** Članarina je aktivna dok datum isteka nije prošao (validUntil >= danas). */
	public boolean jeAktivna(LocalDate danas) {
		return !datumIsteka.isBefore(danas);
	}

	public Integer getId() {
		return id;
	}

	public Double getCijena() {
		return cijena;
	}

	public void setCijena(Double cijena) {
		this.cijena = cijena;
	}

	public LocalDate getDatumPocetka() {
		return datumPocetka;
	}

	public void setDatumPocetka(LocalDate datumPocetka) {
		this.datumPocetka = datumPocetka;
	}

	public LocalDate getDatumIsteka() {
		return datumIsteka;
	}

	public void setDatumIsteka(LocalDate datumIsteka) {
		this.datumIsteka = datumIsteka;
	}
}
