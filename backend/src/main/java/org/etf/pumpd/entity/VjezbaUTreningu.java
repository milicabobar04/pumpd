package org.etf.pumpd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name="vjezba_u_treningu")
public class VjezbaUTreningu {

	protected VjezbaUTreningu() {
		
	}

	public VjezbaUTreningu(Vjezba vjezba, PlanTreninga plan, Integer brojSerija,
			Integer brojPonavljanja) {
		super();
		this.id = new VjezbaUTreninguId(vjezba.getId(),plan.getId());
		this.vjezba = vjezba;
		this.plan = plan;
		this.brojSerija = brojSerija;
		this.brojPonavljanja = brojPonavljanja;
	}

	@EmbeddedId
	private VjezbaUTreninguId id;
	
	@MapsId("vjezbaId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="vjezba_idvjezba")
	private Vjezba vjezba;
	
	@MapsId("planId")
	@ManyToOne(fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="plan_treninga_idplanTreninga")
	private PlanTreninga plan;
	
	@Column(name="brojSerija")
	private Integer brojSerija;
	
	@Column(name="brojPonavljanja")
	private Integer brojPonavljanja;

	public VjezbaUTreninguId getId() {
		return id;
	}

	

	public Vjezba getVjezba() {
		return vjezba;
	}


	public PlanTreninga getPlan() {
		return plan;
	}



	public Integer getBrojSerija() {
		return brojSerija;
	}

	
	public Integer getBrojPonavljanja() {
		return brojPonavljanja;
	}

	public void izmjeni(String naziv,Integer brojSerija,Integer brojPonavljanja)
	{
		this.vjezba.setNaziv(naziv);
		this.brojSerija=brojSerija;
		this.brojPonavljanja=brojPonavljanja;
	}
	
	
	
}
