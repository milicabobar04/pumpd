package org.etf.pumpd.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name="teretana")
public class Teretana {

	public static final int ID=1;
	
	@Id
	@Column( name="idteretana")
	private Integer id;
	
	@Column( name="naziv",length=45)
	private String naziv;
	
	@Column( name="adresa",length=200)
	private String adresa;
	
	@Column( name="vlasnik",length=45)
	private String vlasnik;
	
	@Column( name="radnoVrijeme",length=45)
	private String radnoVrijeme;
	
	@Column( name="opis",length=500)
	private String opis;
	
	@Column( name="kontakt",length=45)
	private String kontakt;
	
	@Column( name="cijenaClanarine")
	private BigDecimal cijenaClanarine;
	
	@Column( name="datumOsnivanja")
	private LocalDate datumOsnivanja;

	@Column( name="kapacitet")
	private Integer kapacitet;
	


	
	public Teretana()
	{
		this.id=ID;
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




	public String getAdresa() {
		return adresa;
	}




	public void setAdresa(String adresa) {
		this.adresa = adresa;
	}




	public String getVlasnik() {
		return vlasnik;
	}




	public void setVlasnik(String vlasnik) {
		this.vlasnik = vlasnik;
	}




	public String getRadnoVrijeme() {
		return radnoVrijeme;
	}




	public void setRadnoVrijeme(String radnoVrijeme) {
		this.radnoVrijeme = radnoVrijeme;
	}




	public String getOpis() {
		return opis;
	}




	public void setOpis(String opis) {
		this.opis = opis;
	}




	public String getKontakt() {
		return kontakt;
	}




	public void setKontakt(String kontakt) {
		this.kontakt = kontakt;
	}




	public BigDecimal getCijenaClanarine() {
		return cijenaClanarine;
	}




	public void setCijenaClanarine(BigDecimal cijenaClanarine) {
		this.cijenaClanarine = cijenaClanarine;
	}




	public LocalDate getDatumOsnivanja() {
		return datumOsnivanja;
	}




	public void setDatumOsnivanja(LocalDate datumOsnivanja) {
		this.datumOsnivanja = datumOsnivanja;
	}




	public Integer getKapacitet() {
		return kapacitet;
	}




	public void setKapacitet(Integer kapacitet) {
		this.kapacitet = kapacitet;
	}


	

	
}