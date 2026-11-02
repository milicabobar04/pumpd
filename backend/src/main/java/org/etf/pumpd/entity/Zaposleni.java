package org.etf.pumpd.entity;
import jakarta.persistence.*;

@Entity
@Table(name="zaposleni")
public class Zaposleni {

	@Id
	@Column( name="nalog_idnalog")
	private Integer id;
	
	@OneToOne(fetch=FetchType.LAZY, optional=false)
	@MapsId
	@JoinColumn(name="nalog_idnalog")
	private Nalog nalog;
	
	protected Zaposleni()
	{
		
	}
	
	public Zaposleni(Nalog nalog)
	{
		this.nalog=nalog;
	}

	public Integer getId() {
		return id;
	}

	public Nalog getNalog() {
		return nalog;
	}


	
	

	
}
