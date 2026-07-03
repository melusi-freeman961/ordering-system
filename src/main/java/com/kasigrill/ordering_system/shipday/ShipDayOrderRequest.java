package com.kasigrill.ordering_system.shipday;

import lombok.Data;

@Data
public class ShipDayOrderRequest {

    private String orderNumber;
    private String customerName;
    private String customerAddress;
    private String customerPhoneNumber;
    private String restaurantName;
    private String restaurantAddress;
    private double totalOrderCost;
    private String deliveryInstructions;
}
