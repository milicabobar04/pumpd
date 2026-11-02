package org.etf.pumpd.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name="clan")
public class Clan {


	public Clan(Nalog nalog, Double tjelesnaTezina, Double visina, String pol, LocalDate datumRodjenja) {
		super();
		this.nalog = nalog;
		this.tjelesnaTezina = tjelesnaTezina;
		this.visina = visina;
		this.pol = pol;
		this.datumRodjenja = datumRodjenja;
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
	
	
	@Column( name="pol", length=45)
	private String pol;

	
	@Column( name="datumRodjenja")
	private LocalDate datumRodjenja;
	

	@Column( name="qrKod", nullable=false, length=64)
	private String qrKod;

	
	protected Clan()
	{
		
	}


	public Integer getId() {
		return id;
	}


	@PrePersist
	void dodijeliQrKodAkoNedostaje()
	{
		if(qrKod==null)
		{
			qrKod= UUID.randomUUID().toString();
		}
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


	public String getPol() {
		return pol;
	}


	public void setPol(String pol) {
		this.pol = pol;
	}


	public LocalDate getDatumRodjenja() {
		return datumRodjenja;
	}


	public void setDatumRodjenja(LocalDate datumRodjenja) {
		this.datumRodjenja = datumRodjenja;
	}


	public String getQrKod() {
		return qrKod;
	}


	public void setQrKod(String qrKod) {
		this.qrKod = qrKod;
	}

	
	


	
	

	
}
