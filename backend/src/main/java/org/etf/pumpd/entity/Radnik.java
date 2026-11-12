package org.etf.pumpd.entity;
import jakarta.persistence.*;

@Entity
@Table(name="radnik")
public class Radnik {

	@Id
	@Column( name="zaposleni_nalog_idnalog")
	private Integer id;
	
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	@MapsId
	@JoinColumn(name="zaposleni_nalog_idnalog")
	private Zaposleni zaposleni;
	
	protected Radnik()
	{
		
	}
	
	public Radnik(Zaposleni zaposleni)
	{
		this.zaposleni=zaposleni;
	}

	public Integer getId() {
		return id;
	}

	public Zaposleni getZaposleni() {
		return zaposleni;
	}


	


	
	

	
}
