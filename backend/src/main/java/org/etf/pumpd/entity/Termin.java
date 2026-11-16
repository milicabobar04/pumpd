package org.etf.pumpd.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Termin grupnog treninga (sessionId iz ugovora). Pripada jednom treneru i ima svoju grupu.
 * Kolona sala postoji u bazi, ali je ugovor ne koristi (ostaje null).
 */
@Entity
@Table(name="termin")
public class Termin {

	public Termin(LocalDateTime vrijeme, Integer trajanjeMinuta, Trener trener, Grupa grupa) {
		super();
		this.vrijeme = vrijeme;
		this.trajanjeMinuta = trajanjeMinuta;
		this.trener = trener;
		this.grupa = grupa;
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="idtermin")
	private Integer id;
	
	@Column(name="Vrijeme")
	private LocalDateTime vrijeme;
	
	@Column(name="Trajanje")
	private Integer trajanjeMinuta;
	
	
	@Column(name="sala",length=45)
	private String sala;
	
	
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="trener_nalog_idnalog")
	private Trener trener;
	
	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="grupa_idgrupa")
	private Grupa grupa;
	
	protected Termin()
	{
		
	}
	
	public void izmijeni(LocalDateTime vrijeme, Integer trajanjeMinuta)
	{
		this.vrijeme=vrijeme;
		this.trajanjeMinuta=trajanjeMinuta;
	}
	
	public LocalDateTime getKraj()
	{
		return vrijeme.plusMinutes(trajanjeMinuta);
	}
	
	public boolean jePoceo(LocalDateTime sada)
	{
		return !vrijeme.isAfter(sada);
	}
	
	public boolean preklapaSe(LocalDateTime pocetak, int trajanje)
	{
		return pocetak.isBefore(getKraj()) && pocetak.plusMinutes(trajanje).isAfter(vrijeme);
	}
	
	public boolean pripadaTreneru(Integer idTrenera)
	{
		return trener.getId().equals(idTrenera);
	}

	public Integer getId() {
		return id;
	}

	public LocalDateTime getVrijeme() {
		return vrijeme;
	}

	public Integer getTrajanjeMinuta() {
		return trajanjeMinuta;
	}

	public String getSala() {
		return sala;
	}

	public Trener getTrener() {
		return trener;
	}

	public Grupa getGrupa() {
		return grupa;
	}
}
