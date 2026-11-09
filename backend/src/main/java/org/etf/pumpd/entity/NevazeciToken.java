package org.etf.pumpd.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name="nevazeci_token")
public class NevazeciToken {

	@Id
	@Column(name="jti", length=36)
	private String jti;

	@Column(name="istice", nullable=false)
	private Instant istice;

	protected NevazeciToken()
	{

	}

	public NevazeciToken(String jti, Instant istice) {
		this.jti = jti;
		this.istice = istice;
	}

	public String getJti() {
		return jti;
	}

	public Instant getIstice() {
		return istice;
	}
}
