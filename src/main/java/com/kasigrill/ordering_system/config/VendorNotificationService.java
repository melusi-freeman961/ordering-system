package com.kasigrill.ordering_system.config;

import com.kasigrill.ordering_system.customer.IncomingVendorMessage;
import com.kasigrill.ordering_system.order.OrderDto;
import com.kasigrill.ordering_system.telegram.VendorMessageData;

public interface VendorNotificationService {

    VendorMessageData sendOrder(OrderDto order);

    void publicOrderNumber(IncomingVendorMessage message, String orderNumber);

    //    boolean requestItemName();
    boolean requestItemPrice();

    boolean sendItemAddedConfirmation();
}
