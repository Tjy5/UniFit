package com.suios.admin.analytics.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class LowConfidenceHotspotsResponse {
    private List<LowConfidenceHotspotDto> hotspots = new ArrayList<>();
}
