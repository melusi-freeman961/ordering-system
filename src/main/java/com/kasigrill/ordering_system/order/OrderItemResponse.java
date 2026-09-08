package com.kasigrill.ordering_system.order;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderItemResponse {
    private String title;
    private Integer quantity;

    public OrderItemResponse(String title, Integer quantity) {
        this.title = title;
        this.quantity = quantity;
    }
}
