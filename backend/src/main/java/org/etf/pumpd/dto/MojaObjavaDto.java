package org.etf.pumpd.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Objava u listi sopstvenih objava trenera (8.5). */
public record MojaObjavaDto(Integer postId, String content, String imageUrl,
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime postTimestamp) {

}
