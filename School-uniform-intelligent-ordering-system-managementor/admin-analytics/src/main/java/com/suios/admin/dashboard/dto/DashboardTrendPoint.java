package com.suios.admin.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendPoint {

    private String day;

    private Long orderCount;
}
