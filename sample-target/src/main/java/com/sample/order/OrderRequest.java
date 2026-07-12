package com.sample.order;

public record OrderRequest(String customerId, String productCode, int quantity) {}
