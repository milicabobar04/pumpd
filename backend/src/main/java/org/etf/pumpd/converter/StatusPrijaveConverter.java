package org.etf.pumpd.converter;


import org.etf.pumpd.enums.StatusPrijave;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusPrijaveConverter implements AttributeConverter<StatusPrijave,String> {

	@Override
	public String convertToDatabaseColumn(StatusPrijave status)
	{
		return status==null? null:status.getDbVrijednost();
	}

	@Override
	public StatusPrijave convertToEntityAttribute(String dbVrijednost) {
		return dbVrijednost == null ? null : StatusPrijave.izBaze(dbVrijednost);
	}
}
