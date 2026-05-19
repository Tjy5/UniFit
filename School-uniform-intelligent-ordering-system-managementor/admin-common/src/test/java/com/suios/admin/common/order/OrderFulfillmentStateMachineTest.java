package com.suios.admin.common.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrderFulfillmentStateMachineTest {

    @Test
    void shouldApplyAdminShipment() {
        FulfillmentState next = OrderFulfillmentStateMachine.transition(
                new FulfillmentState(1L, "PAID", "NOT_SHIPPED"),
                FulfillmentCommand.SHIP_ORDER,
                FulfillmentActorType.ADMIN,
                null
        );

        assertEquals(new FulfillmentState(2L, "PAID", "SHIPPED"), next);
    }

    @Test
    void shouldRejectUserShipmentCommand() {
        FulfillmentState current = new FulfillmentState(1L, "PAID", "NOT_SHIPPED");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                OrderFulfillmentStateMachine.transition(current, FulfillmentCommand.SHIP_ORDER, FulfillmentActorType.USER, null));

        assertEquals("履约命令 SHIP_ORDER 不允许用于当前订单状态", exception.getMessage());
    }

    @Test
    void shouldApplyRefundApprovalForReturnedOrder() {
        FulfillmentState next = OrderFulfillmentStateMachine.transition(
                new FulfillmentState(5L, "REFUNDING", "RETURN_REQUESTED"),
                FulfillmentCommand.APPROVE_REFUND,
                FulfillmentActorType.ADMIN,
                null
        );

        assertEquals(new FulfillmentState(6L, "REFUNDED", "RETURNED"), next);
    }

    @Test
    void shouldDetectInvalidCanonicalCombination() {
        assertFalse(OrderFulfillmentStateMachine.isCanonicalState(new FulfillmentState(3L, "PAID", "SHIPPED")));
    }
}
