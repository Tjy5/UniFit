package com.suios.admin.analytics.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDistributionPageDto {

    private Long total;

    private Integer limit;

    private Integer offset;

    @Builder.Default
    private List<FeedbackDistributionDto> groups = List.of();
}
