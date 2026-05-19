package com.suios.admin.order.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.suios.admin.order.dto.OrderAnomalySummary;
import com.suios.admin.order.entity.SOrders;
import com.suios.admin.order.service.SOrdersService;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class SOrdersControllerTest {

    @Test
    void exportShouldIncludeCanonicalLabelsAndAnomalyMarker() throws Exception {
        SOrdersService service = org.mockito.Mockito.mock(SOrdersService.class);
        SOrdersController controller = new SOrdersController(service);
        SOrders order = new SOrders();
        order.setId(1L);
        order.setUserAccount("user01");
        order.setRecipientName("张三");
        order.setTotalPrice(new BigDecimal("88.00"));
        order.setStatus(1L);
        order.setPaymentStatus("PAID");
        order.setShippingStatus("NOT_SHIPPED");
        OrderAnomalySummary summary = new OrderAnomalySummary();
        summary.setAbnormal(true);
        order.setAnomalySummary(summary);
        when(service.listAll(null, null, null, null)).thenReturn(List.of(order));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(null, null, null, null, response);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(response.getContentAsByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertEquals("待发货", row.getCell(4).getStringCellValue());
            assertEquals("已支付", row.getCell(5).getStringCellValue());
            assertEquals("未发货", row.getCell(6).getStringCellValue());
            assertEquals("异常", row.getCell(7).getStringCellValue());
        }
        assertTrue(response.getContentType().contains("spreadsheetml"));
    }
}
