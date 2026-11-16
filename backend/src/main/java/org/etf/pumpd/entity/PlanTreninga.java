package org.etf.pumpd.entity;

import java.time.LocalDate;
import java.util.*;

import jakarta.persistence.*;

@Entity
@Table(name="plan_treninga")
public class PlanTreninga {

	protected PlanTreninga() {
	
	}

	private PlanTreninga(Integer idTrenera, Integer idClana,LocalDate datumKreiranja) {
		super();
		
		this.datumKreiranja = datumKreiranja;

		this.idTrenera = idTrenera;
		this.idClana = idClana;

	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="idplanTreninga")
	private Integer id;
	
	@Column(name="naziv",length=45)
	private String naziv;
	
	@Column(name="opis", length=500)
	private String opis;
	
	@Column(name="datumKreiranja")
	private LocalDate datumKreiranja;

	@Column(name="trajanjeMinuta", nullable=false)
	private Integer trajanjeMinuta;

	@Column(name="trener_nalog_idnalog")
	private Integer idTrenera;

	@Column(name="clan_nalog_idnalog")
	private Integer idClana;

	@OneToMany(mappedBy="plan", cascade=CascadeType.ALL, orphanRemoval=true)
	private List<VjezbaUTreningu> vjezbe = new ArrayList<>();

	
	public static PlanTreninga zaTrenera(Integer idTrenera, LocalDate danas)
	{
		return new PlanTreninga(idTrenera,null,danas);
	}
	
	public static PlanTreninga zaClana(Integer idClana, LocalDate danas)
	{
		return new PlanTreninga(null,idClana,danas);
	}
	
	public boolean pripada(Integer idKorisnika)
	{
		return idKorisnika.equals(idTrenera)|| idKorisnika.equals(idClana);
	}
	
	public void postaviPodatke(String naziv,String opis, Integer trajanjeMinuta)
	{
		this.naziv=naziv;
		this.opis=opis;
		this.trajanjeMinuta=trajanjeMinuta;
	}
	
	public List<VjezbaUTreningu> getVjezbeUredom()
	{
		return vjezbe.stream().sorted(Comparator.comparing(v->v.getVjezba().getId())).toList();
	}
	public Integer getId() {
		return id;
	}



	public String getNaziv() {
		return naziv;
	}



	public String getOpis() {
		return opis;
	}



	public LocalDate getDatumKreiranja() {
		return datumKreiranja;
	}


	public Integer getTrajanjeMinuta() {
		return trajanjeMinuta;
	}



	public Integer getIdTrenera() {
		return idTrenera;
	}

	

	public Integer getIdClana() {
		return idClana;
	}



	public List<VjezbaUTreningu> getVjezbe() {
		return vjezbe;
	}


}
