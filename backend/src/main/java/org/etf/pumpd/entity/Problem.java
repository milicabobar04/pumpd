package org.etf.pumpd.entity;
import java.time.LocalDateTime;
import org.etf.pumpd.converter.*;
import org.etf.pumpd.enums.*;

import jakarta.persistence.*;

@Entity
@Table(name="problem")
public class Problem {


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idproblem")
	private Integer id;
	
	@Column( name="naslov",nullable=false,length=150)
	private String naslov;
	
	@Column( name="opis",length=500)
	private String opis;
	
	@Convert(converter=StatusProblemaConverter.class)
	@Column( name="status",nullable=false)
	private StatusProblema status;
	
	@Column( name="vrijeme",nullable=false)
	private LocalDateTime vrijeme;


	
	public Problem()
	{
		
	}

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="nalog_idnalog")
	private Nalog prijavio;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="administrator_zaposleni_nalog_idnalog")
	private Administrator obradio;



	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNaslov() {
		return naslov;
	}

	public void setNaslov(String naslov) {
		this.naslov = naslov;
	}

	public String getOpis() {
		return opis;
	}

	public void setOpis(String opis) {
		this.opis = opis;
	}

	public StatusProblema getStatus() {
		return status;
	}

	public void setStatus(StatusProblema status) {
		this.status = status;
	}

	public LocalDateTime getVrijeme() {
		return vrijeme;
	}

	public void setVrijeme(LocalDateTime vrijeme) {
		this.vrijeme = vrijeme;
	}

	public Nalog getPrijavio() {
		return prijavio;
	}

	public void setPrijavio(Nalog prijavio) {
		this.prijavio = prijavio;
	}

	public Administrator getObradio() {
		return obradio;
	}

	public void setObradio(Administrator obradio) {
		this.obradio = obradio;
	}



	

	
}