package com.suios.admin.order.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OrderAnomalySummary {
    private boolean abnormal;
    private List<String> reasons = new ArrayList<>();
}
