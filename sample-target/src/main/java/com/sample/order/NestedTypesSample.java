package com.sample.order;

public class NestedTypesSample {

    public interface OrderPolicy {
        boolean isAllowed(String orderId);
    }

    public enum OrderStatus {
        CREATED, PAID, CANCELLED
    }

    public static class OrderView {
        private final String orderId;

        public OrderView(String orderId) {
            this.orderId = orderId;
        }

        public String getOrderId() {
            return orderId;
        }
    }
}
