package org.etf.pumpd.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name="obavjestenje")
public class Obavjestenje {


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idobavjestenje")
	private Integer id;
	
	@Column( name="sadrzaj",nullable=false,length=1000)
	private String sadrzaj;
	
	@Column( name="datumVrijeme",nullable=false)
	private LocalDateTime datumVrijeme;

	@Column( name="procitano",nullable=false)
	private boolean procitano;
	

	
	public Obavjestenje()
	{
		
	}

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="primalac_nalog_idnalog",nullable=false)
	private Nalog primalac;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="posiljalac_nalog_idnalog")
	private Nalog posiljalac;



	public Integer getId() {
		return id;
	}

	public String getSadrzaj() {
		return sadrzaj;
	}

	public void setSadrzaj(String sadrzaj) {
		this.sadrzaj = sadrzaj;
	}

	public LocalDateTime getDatumVrijeme() {
		return datumVrijeme;
	}

	public void setDatumVrijeme(LocalDateTime datumVrijeme) {
		this.datumVrijeme = datumVrijeme;
	}

	public boolean isProcitano() {
		return procitano;
	}

	public void setProcitano(boolean procitano) {
		this.procitano = procitano;
	}

	public Nalog getPrimalac() {
		return primalac;
	}

	public void setPrimalac(Nalog primalac) {
		this.primalac = primalac;
	}

	public Nalog getPosiljalac() {
		return posiljalac;
	}

	public void setPosiljalac(Nalog posiljalac) {
		this.posiljalac = posiljalac;
	}

	public void setId(int id) {
		this.id=id;
		
	}

	
	

	
}
