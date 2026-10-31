package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ObavjestenjeDto(Integer notificationId,String content,@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime date, @JsonProperty("isSeen") boolean isSeen) {

}
