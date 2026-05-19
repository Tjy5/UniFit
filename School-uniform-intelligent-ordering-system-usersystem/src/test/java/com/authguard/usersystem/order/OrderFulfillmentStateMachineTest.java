package com.authguard.usersystem.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrderFulfillmentStateMachineTest {

    @Test
    void shouldApplyUserPaymentSuccess() {
        FulfillmentState next = OrderFulfillmentStateMachine.transition(
                new FulfillmentState(0L, "PENDING", "NOT_SHIPPED"),
                FulfillmentCommand.SIMULATE_PAYMENT_SUCCESS,
                FulfillmentActorType.USER,
                null
        );

        assertEquals(new FulfillmentState(1L, "PAID", "NOT_SHIPPED"), next);
    }

    @Test
    void shouldRejectShipmentBeforePayment() {
        FulfillmentState current = new FulfillmentState(0L, "PENDING", "NOT_SHIPPED");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                OrderFulfillmentStateMachine.transition(current, FulfillmentCommand.SHIP_ORDER, FulfillmentActorType.ADMIN, null));

        assertEquals("履约命令 SHIP_ORDER 不允许用于当前订单状态", exception.getMessage());
    }

    @Test
    void shouldRestorePreviousStateWhenRefundRejected() {
        FulfillmentState next = OrderFulfillmentStateMachine.transition(
                new FulfillmentState(5L, "REFUNDING", "RETURN_REQUESTED"),
                FulfillmentCommand.REJECT_REFUND,
                FulfillmentActorType.ADMIN,
                new FulfillmentState(2L, "PAID", "SHIPPED")
        );

        assertEquals(new FulfillmentState(2L, "PAID", "SHIPPED"), next);
    }

    @Test
    void shouldDetectInvalidCanonicalCombination() {
        assertFalse(OrderFulfillmentStateMachine.isCanonicalState(new FulfillmentState(3L, "PAID", "SHIPPED")));
    }
}
