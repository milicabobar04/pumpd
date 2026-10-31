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
	private Trener zaposleni;
	
	protected Radnik()
	{
		
	}
	
	public Radnik(Trener zaposleni)
	{
		this.zaposleni=zaposleni;
	}

	public Integer getId() {
		return id;
	}

	public Trener getZaposleni() {
		return zaposleni;
	}


	


	
	

	
}
