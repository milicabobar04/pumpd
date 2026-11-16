package org.etf.pumpd.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VjezbaUTreninguId implements Serializable {

	protected VjezbaUTreninguId() {
		
	}

	public VjezbaUTreninguId(Integer vjezbaId, Integer planId) {
		super();
		this.vjezbaId = vjezbaId;
		this.planId = planId;
	}

	private static final long serialVersionUID = 1L;
	
	@Column(name="vjezba_idvjezba")
	private Integer vjezbaId;
	
	@Column(name="plan_treninga_idplanTreninga")
	private Integer planId;

	public Integer getVjezbaId() {
		return vjezbaId;
	}

	public void setVjezbaId(Integer vjezbaId) {
		this.vjezbaId = vjezbaId;
	}

	public Integer getPlanId() {
		return planId;
	}

	public void setPlanId(Integer planId) {
		this.planId = planId;
	}

	/** Hibernate poredi složene ključeve po vrijednosti, pa su equals i hashCode obavezni. */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VjezbaUTreninguId drugi)) {
			return false;
		}
		return Objects.equals(vjezbaId, drugi.vjezbaId) && Objects.equals(planId, drugi.planId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(vjezbaId, planId);
	}

}
