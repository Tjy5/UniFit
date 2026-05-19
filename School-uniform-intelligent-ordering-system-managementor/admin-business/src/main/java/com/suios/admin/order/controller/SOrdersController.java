package com.suios.admin.order.controller;

import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import com.suios.admin.common.util.ExcelUtils;
import com.suios.admin.common.annotation.OperLog;
import com.suios.admin.common.enums.OrderStatus;
import com.suios.admin.common.enums.PaymentStatus;
import com.suios.admin.common.enums.ShippingStatus;
import com.suios.admin.order.dto.OrderFulfillmentCommandRequest;
import com.suios.admin.order.dto.OrderStatusLogDto;
import com.suios.admin.order.dto.OrderStatusUpdateRequest;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.service.SOrdersService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class SOrdersController {

    private final SOrdersService ordersService;

    @GetMapping("/list")
    public R<PageResult<SOrders>> list(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Long status,
                                       @RequestParam(required = false) String paymentStatus,
                                       @RequestParam(required = false) String shippingStatus) {
        return R.success(ordersService.list(pageNum, pageSize, userId, status, paymentStatus, shippingStatus));
    }

    @GetMapping("/{id}")
    public R<SOrders> getInfo(@PathVariable Long id) {
        return R.success(ordersService.getById(id));
    }

    @PutMapping("/{id}/status")
    @OperLog(module = "订单管理", operation = "UPDATE_STATUS")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateRequest request) {
        ordersService.updateStatus(id, request);
        return R.success("订单状态更新成功");
    }

    @PostMapping("/{id}/fulfillment-commands")
    @OperLog(module = "订单管理", operation = "FULFILLMENT_COMMAND")
    public R<SOrders> executeFulfillmentCommand(@PathVariable Long id,
                                                @Valid @RequestBody OrderFulfillmentCommandRequest request) {
        return R.success("订单履约命令执行成功", ordersService.executeCommand(id, request));
    }

    @GetMapping("/{id}/status-logs")
    public R<List<OrderStatusLogDto>> statusLogs(@PathVariable Long id) {
        return R.success(ordersService.statusLogs(id));
    }

    @GetMapping("/anomalies")
    public R<PageResult<SOrders>> anomalies(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return R.success(ordersService.anomalies(pageNum, pageSize));
    }

    @PostMapping("/export")
    @OperLog(module = "订单管理", operation = "EXPORT")
    public void export(@RequestParam(required = false) Long userId,
                       @RequestParam(required = false) Long status,
                       @RequestParam(required = false) String paymentStatus,
                       @RequestParam(required = false) String shippingStatus,
                       HttpServletResponse response) throws IOException {
        List<SOrders> rows = ordersService.listAll(userId, status, paymentStatus, shippingStatus);
        List<List<?>> dataRows = new ArrayList<>();
        for (SOrders item : rows) {
            dataRows.add(Arrays.asList(
                    item.getId(),
                    item.getUserAccount(),
                    item.getRecipientName(),
                    item.getTotalPrice(),
                    orderStatusLabel(item.getStatus()),
                    paymentStatusLabel(item.getPaymentStatus()),
                    shippingStatusLabel(item.getShippingStatus()),
                    item.getAnomalySummary() != null && item.getAnomalySummary().isAbnormal() ? "异常" : "正常",
                    item.getOrderDate()
            ));
        }
        ExcelUtils.export(
                "orders",
                List.of("订单ID", "用户账号", "收货人", "订单金额", "订单状态", "支付状态", "物流状态", "异常标记", "下单时间"),
                dataRows,
                response
        );
    }

    private String orderStatusLabel(Long status) {
        if (status == null) {
            return "";
        }
        try {
            return OrderStatus.fromCode(status).getLabel();
        } catch (IllegalArgumentException ex) {
            return String.valueOf(status);
        }
    }

    private String paymentStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        try {
            return PaymentStatus.fromCode(status).getLabel();
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    private String shippingStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        try {
            return ShippingStatus.fromCode(status).getLabel();
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }
}
