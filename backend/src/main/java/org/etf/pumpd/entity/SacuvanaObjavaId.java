package org.etf.pumpd.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

/** Složeni ključ tabele sacuvana_objava: (član, objava). Član istu objavu čuva najviše jednom. */
@Embeddable
public class SacuvanaObjavaId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name="clan_nalog_idnalog")
	private Integer clanId;

	@Column(name="objava_idobjava")
	private Integer objavaId;

	protected SacuvanaObjavaId()
	{

	}

	public SacuvanaObjavaId(Integer clanId, Integer objavaId) {
		this.clanId = clanId;
		this.objavaId = objavaId;
	}

	public Integer getClanId() {
		return clanId;
	}

	public Integer getObjavaId() {
		return objavaId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clanId, objavaId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof SacuvanaObjavaId drugi))
			return false;
		return Objects.equals(clanId, drugi.clanId) && Objects.equals(objavaId, drugi.objavaId);
	}
}
