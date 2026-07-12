package com.sample.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public OrderResponse createOrder(OrderRequest request) { return new OrderResponse("ORD-1", "CREATED"); }
    public OrderResponse getOrder(String orderId) { return new OrderResponse(orderId, "CREATED"); }
}
