package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 6.1: date u obliku iz ugovora (YYYY-MM-DDTHH:mm:ss), uvijek sa sekundama. */
public record ObavjestenjeDto(Integer notificationId,String content,@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime date, @JsonProperty("isSeen") boolean isSeen) {

}
