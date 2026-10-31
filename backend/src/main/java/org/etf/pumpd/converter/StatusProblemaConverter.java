package org.etf.pumpd.converter;

import org.etf.pumpd.enums.*;

import jakarta.persistence.*;

@Converter
public class StatusProblemaConverter implements AttributeConverter<StatusProblema,String> {

	@Override
	public String convertToDatabaseColumn(StatusProblema status)
	{
		return status==null? null:status.getDbVrijednost();
	}
	
	@Override
    public StatusProblema convertToEntityAttribute(String dbVrijednost) {
        return dbVrijednost == null ? null : StatusProblema.izBaze(dbVrijednost);
    }
}
