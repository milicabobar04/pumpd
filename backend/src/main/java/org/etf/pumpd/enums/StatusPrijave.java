package org.etf.pumpd.enums;

/**
 * Stanje reda u tabeli clan_na_terminu (vidi V2 migraciju).
 * NA_CEKANJU = član je poslao zahtjev (9.2), PRIJAVLJEN = trener ga je prihvatio ili dodao u grupu,
 * PRISUTAN = prihvaćen i bio je na treningu, ODBIJEN = trener je odbio zahtjev,
 * OTKAZAO = vrijednost iz demo baze; nove rute je ne upisuju (odjava 9.3 briše red).
 * Prihvaćen član (PRIJAVLJEN ili PRISUTAN) je uvijek i u tabeli clan_u_grupi.
 */
public enum StatusPrijave {

	PRIJAVLJEN("prijavljen"),
	OTKAZAO("otkazao"),
	PRISUTAN("prisutan"),
	NA_CEKANJU("na_cekanju"),
	ODBIJEN("odbijen");
	
	private StatusPrijave(String dbVrijednost) {
		this.dbVrijednost = dbVrijednost;
	}

	private final String dbVrijednost;
	
	public boolean jePrihvacen()
	{
		return this==PRIJAVLJEN || this==PRISUTAN;
	}
	
	
	public static StatusPrijave izBaze(String dbVrijednost)
	{
		for(StatusPrijave status: values())
		{
			if(status.dbVrijednost.equals(dbVrijednost))
			{
				return status;
			}
		}
		throw new IllegalArgumentException("Nepoznat status prijave u bazi: "+dbVrijednost);
	}

	public String getDbVrijednost() {
		return dbVrijednost;
	}
}
