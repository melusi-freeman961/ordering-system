package com.kasigrill.ordering_system.config;

import com.kasigrill.ordering_system.customer.IncomingCustomerMessage;
import com.kasigrill.ordering_system.menuitem.MenuItemDto;

import java.util.List;

public interface CustomerNotificationService {

    void publishUnexpectedMessageResponse(String channelId);
    boolean getCustomerNumber(String channelId);

    void publishEmptyMenu(String channelId);

    void publishStoreClosedStatus(String channelId);

    boolean getName(String channelId);

    boolean publishAnotherOrderRequest(String channelId);

    boolean sendOrderStatus(String status, String channelId,String orderNumber);

    boolean sendMenu(IncomingCustomerMessage message, List<MenuItemDto> menu);

    boolean getLocation(String chanelId);
}
