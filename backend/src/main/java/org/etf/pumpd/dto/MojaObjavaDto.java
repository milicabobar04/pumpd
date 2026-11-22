package org.etf.pumpd.dto;

import java.time.LocalDateTime;

/** Objava u listi sopstvenih objava trenera (8.5). */
public record MojaObjavaDto(Integer postId, String content, String imageUrl, LocalDateTime postTimestamp) {

}
