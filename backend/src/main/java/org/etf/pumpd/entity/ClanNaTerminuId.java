package org.etf.pumpd.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

@Embeddable
public class ClanNaTerminuId implements Serializable{

	public ClanNaTerminuId(Integer terminId, Integer clanId) {
		super();
		this.terminId = terminId;
		this.clanId = clanId;
	}

	private static final long serialVersionUID = 1L;

	@Column(name="termin_idtermin")
	private Integer terminId;
	
	@Column(name="clan_nalog_idnalog")
	private Integer clanId;
	
	protected ClanNaTerminuId()
	{
		
	}



	public Integer getTerminId() {
		return terminId;
	}

	public Integer getClanId() {
		return clanId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clanId, terminId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClanNaTerminuId other = (ClanNaTerminuId) obj;
		return Objects.equals(clanId, other.clanId) && Objects.equals(terminId, other.terminId);
	}
}
