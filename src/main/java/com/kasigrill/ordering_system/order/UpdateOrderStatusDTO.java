package com.kasigrill.ordering_system.order;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateOrderStatusDTO {
    private String status;
    private String orderNumber;

    public UpdateOrderStatusDTO(String status, String orderNumber) {
        this.status = status;
        this.orderNumber = orderNumber;
    }
}
