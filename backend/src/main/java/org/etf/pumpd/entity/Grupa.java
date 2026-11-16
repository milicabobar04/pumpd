package org.etf.pumpd.entity;

import jakarta.persistence.*;

/**
 * Grupa grupnog treninga. Svaki termin ima tačno jednu svoju grupu (10.5 ih pravi zajedno),
 * pa su naziv i kapacitet termina zapravo naziv i kapacitet grupe.
 * Članovi grupe su u tabeli clan_u_grupi (ClanUGrupi).
 */
@Entity
@Table(name="grupa")
public class Grupa {

	public Grupa(String naziv, Integer maxClanova, Trener trener) {
		super();
		this.naziv = naziv;
		this.maxClanova = maxClanova;
		this.trener = trener;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="idgrupa")
	private Integer id;
	
	@Column(name="naziv",length=45)
	private String naziv;
	
	@Column(name="opis",length=500)
	private String opis;
	
	@Column(name="maxClanova")
	private Integer maxClanova;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="trener_nalog_idnalog")
	private Trener trener;

	protected Grupa()
	{

	}
	
	public void izmjeni(String naziv,Integer maxClanova)
	{
		this.naziv=naziv;
		this.maxClanova=maxClanova;
	}

	public Integer getId() {
		return id;
	}

	public String getNaziv() {
		return naziv;
	}

	public String getOpis() {
		return opis;
	}

	public Integer getMaxClanova() {
		return maxClanova;
	}

	public Trener getTrener() {
		return trener;
	}
}
