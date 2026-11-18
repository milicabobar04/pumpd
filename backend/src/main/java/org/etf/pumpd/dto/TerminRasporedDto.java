package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record TerminRasporedDto(
		Integer sessionId,String name,
		@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")LocalDateTime startTime,
		Integer durationMinutes,long enrolledCount,Integer maxMembers)
{

}
