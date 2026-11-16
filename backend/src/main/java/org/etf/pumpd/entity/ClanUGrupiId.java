package org.etf.pumpd.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

/** Složeni ključ tabele clan_u_grupi: (član, grupa). */
@Embeddable
public class ClanUGrupiId implements Serializable{

	public ClanUGrupiId(Integer clanId, Integer grupaId) {
		super();
		this.clanId = clanId;
		this.grupaId = grupaId;
	}

	private static final long serialVersionUID = 1L;

	@Column(name="clan_nalog_idnalog")
	private Integer clanId;
	
	@Column(name="grupa_idgrupa")
	private Integer grupaId;
	
	protected ClanUGrupiId()
	{
		
	}

	@Override
	public int hashCode() {
		return Objects.hash(clanId, grupaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClanUGrupiId other = (ClanUGrupiId) obj;
		return Objects.equals(clanId, other.clanId) && Objects.equals(grupaId, other.grupaId);
	}

	
	public Integer getClanId() {
		return clanId;
	}

	public Integer getGrupaId() {
		return grupaId;
	}
	
}
