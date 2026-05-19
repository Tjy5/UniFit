package com.suios.admin.analytics.dto;

import java.util.List;
import lombok.Data;

@Data
public class StalledCartsResponse {

    private List<StalledCartRowDto> records = List.of();

    private Long total = 0L;

    private Integer limit;

    private Integer offset;
}
