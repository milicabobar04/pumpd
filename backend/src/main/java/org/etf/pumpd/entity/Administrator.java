package org.etf.pumpd.entity;
import jakarta.persistence.*;

@Entity
@Table(name="administrator")
public class Administrator {

	@Id
	@Column( name="zaposleni_nalog_idnalog")
	private Integer id;
	
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	@MapsId
	@JoinColumn(name="zaposleni_nalog_idnalog")
	private Radnik zaposleni;
	
	protected Administrator()
	{
		
	}
	
	public Administrator(Radnik zaposleni)
	{
		this.zaposleni=zaposleni;
	}

	public Integer getId() {
		return id;
	}

	
	public Radnik getZaposleni() {
		return zaposleni;
	}

	
}
