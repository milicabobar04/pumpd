package org.etf.pumpd.entity;

import jakarta.persistence.*;

@Entity
@Table(name="trener")
public class Trener {

	public Trener(Nalog nalog, Double tjelesnaTezina, Double visina, String opis) {
		super();
		this.nalog = nalog;
		this.tjelesnaTezina = tjelesnaTezina;
		this.visina = visina;
		this.opis = opis;
	}

	@Id
	@Column( name="nalog_idnalog")
	private Integer id;
	
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	@MapsId
	@JoinColumn(name="nalog_idnalog")
	private Nalog nalog;
	
	@Column( name="tjelesnaTezina")
	private Double tjelesnaTezina;
	
	@Column( name="visina")
	private Double visina;
	
	
	@Column( name="opis", length=500)
	private String opis;
	
	protected Trener()
	{
		
	}

	public Integer getId() {
		return id;
	}

	public Nalog getNalog() {
		return nalog;
	}

	public void setNalog(Nalog nalog) {
		this.nalog = nalog;
	}

	public Double getTjelesnaTezina() {
		return tjelesnaTezina;
	}

	public void setTjelesnaTezina(Double tjelesnaTezina) {
		this.tjelesnaTezina = tjelesnaTezina;
	}

	public Double getVisina() {
		return visina;
	}

	public void setVisina(Double visina) {
		this.visina = visina;
	}

	public String getOpis() {
		return opis;
	}

	public void setOpis(String opis) {
		this.opis = opis;
	}
	
	


	
	

	
}
