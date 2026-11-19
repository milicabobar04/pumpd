package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record GrupaDto(Integer groupId,Integer sessionId,String name,@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")LocalDateTime startTime,long memberCount,Integer maxMembers) {

}
