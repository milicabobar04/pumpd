package org.etf.pumpd.dto;

import java.util.List;

public record PlanDetaljiDto(Integer planId,String name,String description,Integer durationMinutes,List<VjezbaDto> exercises) {

}
