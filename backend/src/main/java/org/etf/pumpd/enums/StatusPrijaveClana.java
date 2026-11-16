package org.etf.pumpd.enums;

/**
 * enrollmentStatus iz ugovora (9.1): odnos prijavljenog člana i termina, za dugme
 * „Pošalji zahtjev / Čeka / Prijavljen / Odbijen“. Nema reda u clan_na_terminu = NONE.
 * OTKAZAO se prikazuje kao NONE, jer član može poslati novi zahtjev.
 */
public enum StatusPrijaveClana {


	NONE,
	PENDING,
	ACCEPTED,
	REJECTED;
	
	public static StatusPrijaveClana iz(StatusPrijave statusUBazi) {
		if(statusUBazi==null)
		{	
			return NONE;
		}
		return switch(statusUBazi)
		{
			case NA_CEKANJU->PENDING;
			case PRIJAVLJEN,PRISUTAN->ACCEPTED;
			case ODBIJEN->REJECTED;
			case OTKAZAO->NONE;
		
		};
	}

	
	
	
}
