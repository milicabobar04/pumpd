package org.etf.pumpd.entity;

import java.time.LocalDate;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.*;

/**
 * Objava koju je član sačuvao (8.2–8.4).
 * Persistable: ključ (član, objava) postavljamo sami, pa bi Spring Data inače pri save() pretpostavio
 * da red postoji i uradio merge (tihu izmjenu). Ovako novi objekat uvijek ide kao INSERT, pa baza
 * (primarni ključ) odbije drugi upis istog para i kod istovremenog dvostrukog klika (8.2 → 409).
 */
@Entity
@Table(name="sacuvana_objava")
public class SacuvanaObjava implements Persistable<SacuvanaObjavaId> {

	@EmbeddedId
	private SacuvanaObjavaId id;

	@MapsId("clanId")
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="clan_nalog_idnalog")
	private Clan clan;

	@MapsId("objavaId")
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="objava_idobjava")
	private Objava objava;

	@Column(name="datumCuvanja")
	private LocalDate datumCuvanja;

	/** true za objekat napravljen sa new; false nakon upisa ili učitavanja iz baze. */
	@Transient
	private boolean nova = true;

	protected SacuvanaObjava()
	{

	}

	/** Ključ se pravi iz člana i objave, pa ne može biti pogrešan (objava mora već imati id). */
	public SacuvanaObjava(Clan clan, Objava objava, LocalDate datumCuvanja) {
		this.id = new SacuvanaObjavaId(clan.getNalog().getId(), objava.getId());
		this.clan = clan;
		this.objava = objava;
		this.datumCuvanja = datumCuvanja;
	}

	@Override
	public SacuvanaObjavaId getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return nova;
	}

	@PostLoad
	@PostPersist
	void oznaciKaoPostojecu() {
		this.nova = false;
	}

	public Clan getClan() {
		return clan;
	}

	public Objava getObjava() {
		return objava;
	}

	public LocalDate getDatumCuvanja() {
		return datumCuvanja;
	}
}
