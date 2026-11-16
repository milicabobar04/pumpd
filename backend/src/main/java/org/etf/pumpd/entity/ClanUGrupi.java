package org.etf.pumpd.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

/**
 * Član grupe. Pravilo za oba toka: prihvaćen član termina (ClanNaTerminu sa statusom
 * PRIJAVLJEN ili PRISUTAN) uvijek ima i ovaj red, a broj ovih redova je broj zauzetih mjesta.
 */
@Entity
@Table(name="clan_u_grupi")
public class ClanUGrupi {

	/** Ključ se pravi iz člana i grupe, pa ne može biti pogrešan (grupa mora već imati id). */
	public ClanUGrupi(Clan clan, Grupa grupa, LocalDate datumPristupanja) {
		super();
		this.id = new ClanUGrupiId(clan.getNalog().getId(), grupa.getId());
		this.clan = clan;
		this.grupa = grupa;
		this.datumPristupanja = datumPristupanja;
	}



	@EmbeddedId
	private ClanUGrupiId id;
	
	@MapsId("clanId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;
	
	@MapsId("grupaId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="grupa_idgrupa")
	private Grupa grupa;
	
	@Column(name="datumPristupanja")
	private LocalDate datumPristupanja;

	protected ClanUGrupi()
	{
		
	}

	public ClanUGrupiId getId() {
		return id;
	}

	public Clan getClan() {
		return clan;
	}

	public Grupa getGrupa() {
		return grupa;
	}

	public LocalDate getDatumPristupanja() {
		return datumPristupanja;
	}

	
	
}
