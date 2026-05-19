package com.authguard.usersystem.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OrderFulfillmentStateMachine {

    private OrderFulfillmentStateMachine() {
    }

    public static List<FulfillmentCommand> allowedCommands(FulfillmentState current, FulfillmentActorType actorType) {
        List<FulfillmentCommand> commands = new ArrayList<>();
        for (FulfillmentCommand command : commandSetForActor(actorType)) {
            if (canApply(current, command, actorType)) {
                commands.add(command);
            }
        }
        return commands;
    }

    public static boolean canApply(FulfillmentState current, FulfillmentCommand command, FulfillmentActorType actorType) {
        if (!actorCanRun(command, actorType)) {
            return false;
        }
        return switch (command) {
            case SIMULATE_PAYMENT_SUCCESS, CANCEL_UNPAID ->
                    isPendingPayment(current) && isPendingOrFailedPayment(current) && isNotShipped(current);
            case SIMULATE_PAYMENT_FAILURE -> isPendingPayment(current) && isNotShipped(current);
            case SHIP_ORDER -> isWaitShip(current) && isPaid(current) && isNotShipped(current);
            case CONFIRM_RECEIPT -> isShipped(current) && isPaid(current) && shippingIs(current, ShippingStatus.SHIPPED);
            case REQUEST_REFUND -> isRefundableOrder(current) && isPaid(current);
            case APPROVE_REFUND, REJECT_REFUND -> isRefunding(current) && paymentIs(current, PaymentStatus.REFUNDING);
            case LEGACY_STATUS_IMPORTED, ORDER_CREATED -> actorType == FulfillmentActorType.SYSTEM;
        };
    }

    public static FulfillmentState transition(FulfillmentState current,
                                              FulfillmentCommand command,
                                              FulfillmentActorType actorType,
                                              FulfillmentState previousBeforeRefund) {
        if (!canApply(current, command, actorType)) {
            throw new IllegalArgumentException("履约命令 " + command + " 不允许用于当前订单状态");
        }
        return switch (command) {
            case SIMULATE_PAYMENT_SUCCESS -> new FulfillmentState(
                    OrderStatus.WAIT_SHIP.getCode(),
                    PaymentStatus.PAID.getCode(),
                    ShippingStatus.NOT_SHIPPED.getCode()
            );
            case SIMULATE_PAYMENT_FAILURE -> new FulfillmentState(
                    OrderStatus.PENDING_PAYMENT.getCode(),
                    PaymentStatus.FAILED.getCode(),
                    ShippingStatus.NOT_SHIPPED.getCode()
            );
            case CANCEL_UNPAID -> new FulfillmentState(
                    OrderStatus.CANCELLED.getCode(),
                    PaymentStatus.CLOSED.getCode(),
                    ShippingStatus.NOT_SHIPPED.getCode()
            );
            case SHIP_ORDER -> new FulfillmentState(
                    OrderStatus.SHIPPED.getCode(),
                    PaymentStatus.PAID.getCode(),
                    ShippingStatus.SHIPPED.getCode()
            );
            case CONFIRM_RECEIPT -> new FulfillmentState(
                    OrderStatus.COMPLETED.getCode(),
                    PaymentStatus.PAID.getCode(),
                    ShippingStatus.DELIVERED.getCode()
            );
            case REQUEST_REFUND -> new FulfillmentState(
                    OrderStatus.REFUNDING.getCode(),
                    PaymentStatus.REFUNDING.getCode(),
                    requiresReturn(current) ? ShippingStatus.RETURN_REQUESTED.getCode() : ShippingStatus.NOT_SHIPPED.getCode()
            );
            case APPROVE_REFUND -> new FulfillmentState(
                    OrderStatus.REFUNDED.getCode(),
                    PaymentStatus.REFUNDED.getCode(),
                    shippingIs(current, ShippingStatus.RETURN_REQUESTED)
                            ? ShippingStatus.RETURNED.getCode()
                            : ShippingStatus.NOT_SHIPPED.getCode()
            );
            case REJECT_REFUND -> restorePreviousRefundState(previousBeforeRefund);
            case LEGACY_STATUS_IMPORTED, ORDER_CREATED -> current;
        };
    }

    public static Optional<FulfillmentCommand> findMatchingCommand(FulfillmentState current,
                                                                  FulfillmentActorType actorType,
                                                                  FulfillmentState target,
                                                                  FulfillmentState previousBeforeRefund) {
        return allowedCommands(current, actorType).stream()
                .filter(command -> matchesTarget(current, command, actorType, target, previousBeforeRefund))
                .findFirst();
    }

    public static boolean isCanonicalState(FulfillmentState state) {
        if (!OrderStatus.supports(state.orderStatus())
                || !PaymentStatus.supports(state.paymentStatus())
                || !ShippingStatus.supports(state.shippingStatus())) {
            return false;
        }
        if (isPendingPayment(state)) {
            return isPendingOrFailedPayment(state) && isNotShipped(state);
        }
        if (isWaitShip(state)) {
            return isPaid(state) && isNotShipped(state);
        }
        if (isShipped(state)) {
            return isPaid(state) && shippingIs(state, ShippingStatus.SHIPPED);
        }
        if (orderIs(state, OrderStatus.COMPLETED)) {
            return isPaid(state) && shippingIs(state, ShippingStatus.DELIVERED);
        }
        if (orderIs(state, OrderStatus.CANCELLED)) {
            return paymentIs(state, PaymentStatus.CLOSED) && isNotShipped(state);
        }
        if (isRefunding(state)) {
            return paymentIs(state, PaymentStatus.REFUNDING)
                    && (isNotShipped(state) || shippingIs(state, ShippingStatus.RETURN_REQUESTED));
        }
        if (orderIs(state, OrderStatus.REFUNDED)) {
            return paymentIs(state, PaymentStatus.REFUNDED)
                    && (isNotShipped(state) || shippingIs(state, ShippingStatus.RETURNED));
        }
        return false;
    }

    private static List<FulfillmentCommand> commandSetForActor(FulfillmentActorType actorType) {
        return switch (actorType) {
            case USER -> List.of(
                    FulfillmentCommand.SIMULATE_PAYMENT_SUCCESS,
                    FulfillmentCommand.SIMULATE_PAYMENT_FAILURE,
                    FulfillmentCommand.CANCEL_UNPAID,
                    FulfillmentCommand.CONFIRM_RECEIPT,
                    FulfillmentCommand.REQUEST_REFUND
            );
            case ADMIN -> List.of(
                    FulfillmentCommand.SHIP_ORDER,
                    FulfillmentCommand.CONFIRM_RECEIPT,
                    FulfillmentCommand.APPROVE_REFUND,
                    FulfillmentCommand.REJECT_REFUND
            );
            case SYSTEM -> List.of(FulfillmentCommand.LEGACY_STATUS_IMPORTED, FulfillmentCommand.ORDER_CREATED);
        };
    }

    private static boolean matchesTarget(FulfillmentState current,
                                         FulfillmentCommand command,
                                         FulfillmentActorType actorType,
                                         FulfillmentState target,
                                         FulfillmentState previousBeforeRefund) {
        try {
            return transition(current, command, actorType, previousBeforeRefund).equals(target);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean actorCanRun(FulfillmentCommand command, FulfillmentActorType actorType) {
        return commandSetForActor(actorType).contains(command);
    }

    private static FulfillmentState restorePreviousRefundState(FulfillmentState previousBeforeRefund) {
        if (previousBeforeRefund == null) {
            throw new IllegalArgumentException("拒绝退款缺少退款申请前的状态审计记录");
        }
        if (!isRefundableOrder(previousBeforeRefund)) {
            throw new IllegalArgumentException("退款申请前状态无法恢复");
        }
        return new FulfillmentState(
                previousBeforeRefund.orderStatus(),
                PaymentStatus.PAID.getCode(),
                previousBeforeRefund.shippingStatus()
        );
    }

    private static boolean isPendingPayment(FulfillmentState state) {
        return orderIs(state, OrderStatus.PENDING_PAYMENT);
    }

    private static boolean isWaitShip(FulfillmentState state) {
        return orderIs(state, OrderStatus.WAIT_SHIP);
    }

    private static boolean isShipped(FulfillmentState state) {
        return orderIs(state, OrderStatus.SHIPPED);
    }

    private static boolean isRefunding(FulfillmentState state) {
        return orderIs(state, OrderStatus.REFUNDING);
    }

    private static boolean isRefundableOrder(FulfillmentState state) {
        return orderIs(state, OrderStatus.WAIT_SHIP)
                || orderIs(state, OrderStatus.SHIPPED)
                || orderIs(state, OrderStatus.COMPLETED);
    }

    private static boolean isPaid(FulfillmentState state) {
        return paymentIs(state, PaymentStatus.PAID);
    }

    private static boolean isPendingOrFailedPayment(FulfillmentState state) {
        return paymentIs(state, PaymentStatus.PENDING) || paymentIs(state, PaymentStatus.FAILED);
    }

    private static boolean isNotShipped(FulfillmentState state) {
        return shippingIs(state, ShippingStatus.NOT_SHIPPED);
    }

    private static boolean requiresReturn(FulfillmentState state) {
        return shippingIs(state, ShippingStatus.SHIPPED) || shippingIs(state, ShippingStatus.DELIVERED);
    }

    private static boolean orderIs(FulfillmentState state, OrderStatus status) {
        return status.getCode().equals(state.orderStatus());
    }

    private static boolean paymentIs(FulfillmentState state, PaymentStatus status) {
        return status.getCode().equals(state.paymentStatus());
    }

    private static boolean shippingIs(FulfillmentState state, ShippingStatus status) {
        return status.getCode().equals(state.shippingStatus());
    }
}
