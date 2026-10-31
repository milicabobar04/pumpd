package org.etf.pumpd.entity;

import jakarta.persistence.*;

@Entity
@Table(name="nalog")
public class Nalog {

	public Nalog(String ime, String prezime, String email, String lozinka) {
		super();
		this.ime = ime;
		this.prezime = prezime;
		this.email = email;
		this.lozinka = lozinka;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idnalog")
	private Integer id;
	
	@Column( name="ime",nullable=false,length=45)
	private String ime;
	
	@Column( name="prezime",nullable=false,length=45)
	private String prezime;

	@Column( name="email",nullable=false,length=100)
	private String email;
	
	@Column( name="lozinka",nullable=false,length=255)
	private String lozinka;
	
	protected Nalog()
	{
		
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getIme() {
		return ime;
	}

	public void setIme(String ime) {
		this.ime = ime;
	}

	public String getPrezime() {
		return prezime;
	}

	public void setPrezime(String prezime) {
		this.prezime = prezime;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getLozinka() {
		return lozinka;
	}

	public void setLozinka(String lozinka) {
		this.lozinka = lozinka;
	}
	
	

	
	

	
}
