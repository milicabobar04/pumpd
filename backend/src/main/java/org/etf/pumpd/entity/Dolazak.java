package org.etf.pumpd.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Jedan boravak člana u teretani (11.8 ulazak, 11.9 izlazak, 5.3 zauzetost).
 * vrijemeIzlaska = null znači da je član još u teretani.
 */
@Entity
@Table(name="dolazak")
public class Dolazak {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="iddolazak")
	private Integer id;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;

	@Column(name="vrijemeUlaska", nullable=false)
	private LocalDateTime vrijemeUlaska;

	@Column(name="vrijemeIzlaska")
	private LocalDateTime vrijemeIzlaska;

	protected Dolazak()
	{

	}

	public Dolazak(Clan clan, LocalDateTime vrijemeUlaska) {
		this.clan = clan;
		this.vrijemeUlaska = vrijemeUlaska;
	}

	public void evidentirajIzlazak(LocalDateTime vrijeme) {
		this.vrijemeIzlaska = vrijeme;
	}

	public Integer getId() {
		return id;
	}

	public Clan getClan() {
		return clan;
	}

	public LocalDateTime getVrijemeUlaska() {
		return vrijemeUlaska;
	}

	public LocalDateTime getVrijemeIzlaska() {
		return vrijemeIzlaska;
	}
}
