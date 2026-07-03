package com.kasigrill.ordering_system.whatsapp;

import com.kasigrill.ordering_system.config.CustomerNotificationService;
import com.kasigrill.ordering_system.customer.IncomingCustomerMessage;
import com.kasigrill.ordering_system.menuitem.MenuItemDto;
import com.kasigrill.ordering_system.resturant.StoreService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WhatsAppService implements CustomerNotificationService {


    private final StoreService storeService;
    private final WhatsappPayloadParser payloadParser;
    private final WhatsAppGateway whatsAppGateway;

    public WhatsAppService(@Lazy StoreService storeService
            , WhatsAppGateway whatsAppGateway
            , WhatsappPayloadParser payloadParser
    ) {
        this.storeService = storeService;
        this.payloadParser = payloadParser;
        this.whatsAppGateway = whatsAppGateway;
    }

    public void deserializeAndProcessWhatsappMessage(Map<String, Object> payload) {
        boolean updatedStatus = payloadParser.isStatusUpdatePayload(payload);

        if (updatedStatus) {
            return;
        }
        WhatsappMessageDto deserializedMessage = payloadParser.parsePayload(payload);

        storeService.executeCustomerMessage(deserializedMessage);


    }

    @Override
    public boolean sendMenu(IncomingCustomerMessage message, List<MenuItemDto> menu) {
        return whatsAppGateway.menuResponseMessage(message.getMessage(), message.getChannelId(), menu);
    }

    @Override
    public void publishStoreClosedStatus(String customerNumber) {
        whatsAppGateway.storeClosedMessage(customerNumber);
    }


    @Override
    public void publishUnexpectedMessageResponse(String channelId) {
        whatsAppGateway.requestAnotherOrder(channelId);
    }

    @Override
    public boolean getCustomerNumber(String channelId) {
        return whatsAppGateway.getCustomerNumber(channelId);
    }

    @Override
    public void publishEmptyMenu(String customerNumber) {
        whatsAppGateway.menuEmptyNotification(customerNumber);
    }

    @Override
    public boolean getName(String customerNumber) {
        return whatsAppGateway.requestCustomerName(customerNumber);
    }

    @Override
    public boolean publishAnotherOrderRequest(String channelId) {
        return whatsAppGateway.requestAnotherOrder(channelId);
    }

    @Override
    public boolean sendOrderStatus(String status, String channelId,String orderNumber) {
       return whatsAppGateway.sendOrderStatus(status, channelId,orderNumber);
    }


    @Override
    public boolean getLocation(String customerNumber) {
        return whatsAppGateway.requestLocation(customerNumber);
    }

}
