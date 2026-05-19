package com.suios.admin.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DashboardRecentOrder {

    private Long id;

    private String userAccount;

    private BigDecimal totalPrice;

    private Long status;

    private LocalDateTime orderDate;
}
