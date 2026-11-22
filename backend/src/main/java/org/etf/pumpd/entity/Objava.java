package org.etf.pumpd.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Objava trenera (8.1–8.7). Mapiranje na ugovor (V2): sadrzaj = content, vrijeme = postTimestamp,
 * slikaPath = imageUrl. U bazi su sadrzaj i vrijeme NULL (stara demo šema), ali ih aplikacija
 * uvijek popunjava. Brisanje objave je trajno (zajedno sa njenim sačuvanim redovima).
 */
@Entity
@Table(name="objava")
public class Objava {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column( name="idobjava")
	private Integer id;

	@ManyToOne(fetch=FetchType.LAZY, optional=false)
	@JoinColumn(name="trener_nalog_idnalog")
	private Trener trener;

	@Column( name="sadrzaj", nullable=false, length=500)
	private String sadrzaj;

	@Column( name="vrijeme", nullable=false)
	private LocalDateTime vrijeme;

	@Column( name="slikaPath", length=255)
	private String slikaPath;

	protected Objava()
	{

	}

	public Objava(Trener trener, String sadrzaj, String slikaPath, LocalDateTime vrijeme) {
		this.trener = trener;
		this.sadrzaj = sadrzaj;
		this.slikaPath = slikaPath;
		this.vrijeme = vrijeme;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Trener getTrener() {
		return trener;
	}

	public String getSadrzaj() {
		return sadrzaj;
	}

	public LocalDateTime getVrijeme() {
		return vrijeme;
	}

	public String getSlikaPath() {
		return slikaPath;
	}
}
