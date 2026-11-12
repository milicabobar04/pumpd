package org.etf.pumpd.converter;

import org.etf.pumpd.enums.*;

import jakarta.persistence.*;

@Converter
public class StatusPracenjaConverter implements AttributeConverter<StatusPracenja,String> {

	@Override
	public String convertToDatabaseColumn(StatusPracenja status)
	{
		return status==null? null:status.getDbVrijednost();
	}

	@Override
	public StatusPracenja convertToEntityAttribute(String dbVrijednost) {
		return dbVrijednost == null ? null : StatusPracenja.izBaze(dbVrijednost);
	}
}
