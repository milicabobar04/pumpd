package org.etf.pumpd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="vjezba")
public class Vjezba {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="idvjezba")
	private Integer id;
	
	@Column(name="naziv",length=45)
	private String naziv;
	
	protected Vjezba()
	{
		
	}
	
	public Vjezba (String naziv)
	{
		this.naziv=naziv;
	}

	public Integer getId() {
		return id;
	}


	public String getNaziv() {
		return naziv;
	}

	public void setNaziv(String naziv) {
		this.naziv = naziv;
	}

}
