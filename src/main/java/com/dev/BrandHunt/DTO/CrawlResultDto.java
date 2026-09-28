package com.dev.BrandHunt.DTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrawlResultDto {
    private int total;
    private int inserted;
    private int updated;
}
