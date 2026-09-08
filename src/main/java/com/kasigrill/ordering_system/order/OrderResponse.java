package com.kasigrill.ordering_system.order;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;


@Setter
@Getter
public class OrderResponse {
    private Long id;
    private String orderNumber;

    private String customerName;
    private String customerMobile;

    private BigDecimal orderAmount;

    private String status;
    private List<OrderItemResponse> items;

    public OrderResponse(Long id, String orderNumber, String customerName, String customerMobile, BigDecimal orderAmount, String status, List<OrderItemResponse> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.customerMobile = customerMobile;
        this.orderAmount = orderAmount;
        this.status = status;
        this.items = items;
    }
}
